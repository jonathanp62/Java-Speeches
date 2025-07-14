package net.jmp.speeches.load;

/*
 * (#)Load.java 0.4.0   07/12/2025
 * (#)Load.java 0.3.0   07/08/2025
 * (#)Load.java 0.1.0   07/05/2025
 *
 * @author   Jonathan Parker
 *
 * MIT License
 *
 * Copyright (c) 2025 Jonathan M. Parker
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import com.mongodb.client.model.Projections;

import io.pinecone.clients.Index;
import io.pinecone.clients.Pinecone;

import io.pinecone.proto.DescribeIndexStatsResponse;
import io.pinecone.proto.NamespaceSummary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.jmp.speeches.Operation;

import net.jmp.speeches.documents.MongoSpeechDocument;
import net.jmp.speeches.documents.MongoVectorDocument;

import net.jmp.speeches.text.TextAnalyzerResponse;
import net.jmp.speeches.text.TextSplitter;
import net.jmp.speeches.text.TextSplitterResponse;

import static net.jmp.util.logging.LoggerUtils.*;

import org.bson.conversions.Bson;

import org.openapitools.db_data.client.ApiException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The load Pinecone index from MongoDB class.
///
/// @version    0.4.0
/// @since      0.1.0
public final class Load extends Operation {
    /// The logger.
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /// The constructor.
    ///
    /// @param  builder net.jmp.speeches.load.Load.Builder
    public Load(final Builder builder) {
        super(Operation.operationBuilder()
                .pinecone(builder.pinecone)
                .searchableEmbeddingModel(builder.searchableEmbeddingModel)
                .searchableIndexName(builder.searchableIndexName)
                .namespace(builder.namespace)
                .mongoClient(builder.mongoClient)
                .speechesCollectionName(builder.speechesCollectionName)
                .vectorsCollectionName(builder.vectorsCollectionName)
                .dbName(builder.dbName)
                .maxTokens(builder.maxTokens)
                .loadTimeoutInSeconds(builder.timeoutInSeconds)
        );
    }

    /// Return the builder.
    ///
    /// @return  net.jmp.speeches.load.Load.Builder
    public static Builder builder() {
        return new Builder();
    }

    /// The operate method.
    @Override
    public void operate() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        this.logger.info("Searchable index: {}", this.searchableIndexName);
        this.logger.info("Embedding model : {}", this.searchableEmbeddingModel);
        this.logger.info("Namespace       : {}", this.namespace);

        if (this.doesSearchableIndexExist() && !this.isSearchableIndexLoaded()) {
            this.logger.info("Loading searchable index: {}", this.searchableIndexName);

            final List<MongoSpeechDocument> speechDocuments = this.getSpeechDocuments();

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Documents fetched: {}", speechDocuments.size());
            }

            this.processDocuments(speechDocuments);
        } else {
            this.logger.info("Searchable index either does not exist or is already loaded: {}", this.searchableIndexName);
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Process the speech documents.
    ///
    /// @param  speechDocuments   java.util.List<net.jmp.speeches.documents.MongoSpeechDocument>
    private void processDocuments(final List<MongoSpeechDocument> speechDocuments) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(speechDocuments));
        }

        int totalTextSegments = 0;

        for (final MongoSpeechDocument document : speechDocuments) {
            final List<String> textSegments = this.getTextSegments(document.getTextAnalysis().getText());

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Text segments: {}", textSegments.size());
            }

            totalTextSegments += textSegments.size();

            this.embedDocument(document, textSegments);
        }

        this.logger.info("Total text segments: {}", totalTextSegments);

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Embed the speech document.
    ///
    /// @param  speechDocument  net.jmp.speeches.documents.MongoSpeechDocument
    /// @param  textSegments    java.util.List<java.lang.String>
    private void embedDocument(final MongoSpeechDocument speechDocument, final List<String> textSegments) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(speechDocument, textSegments));
        }

        final List<Map<String, String>> upsertRecords = new ArrayList<>();
        final String mongoId = speechDocument.getId();
        final TextAnalyzerResponse textAnalysis = speechDocument.getTextAnalysis();
        final String author = textAnalysis.getAuthor();
        final String title = textAnalysis.getTitle();

        for (final String textSegment : textSegments) {
            final Map<String, String> upsertRecord = new HashMap<>();

            upsertRecord.put("_id", UUID.randomUUID().toString());
            upsertRecord.put("text_segment", textSegment);
            upsertRecord.put("title", title);
            upsertRecord.put("author", author);
            upsertRecord.put("mongoid", mongoId);

            upsertRecords.add(upsertRecord);
        }

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Batching {} records for {}", upsertRecords.size(), speechDocument.getId());
        }

        final int maxBatchSize = 96;
        final int numBatches = (int) Math.ceil((double) upsertRecords.size() / maxBatchSize);
        final List<List<Map<String, String>>> batches = new ArrayList<>(numBatches);

        List<Map<String, String>> batchInUse = null;

        for (int i = 0; i < upsertRecords.size(); i++) {
            if (i % maxBatchSize == 0) {
                batchInUse = new ArrayList<>();

                batches.add(batchInUse);
            }

            batchInUse.add(upsertRecords.get(i));
        }

        for (final List<Map<String, String>> batch : batches) {
            try (final Index index = this.pinecone.getIndexConnection(this.searchableIndexName)) {
                final int vectorCountExpected = this.getVectorCount(index) + batch.size();

                index.upsertRecords(this.namespace, batch);
                this.waitUntilUpsertIsComplete(index, vectorCountExpected);
                this.logger.info("Upserted {} records for {}", batch.size(), speechDocument.getId());
            } catch (final ApiException | LoadException e) {
                this.logger.error(catching(e));
            }
        }

        this.insertVectorDocument(speechDocument, upsertRecords);

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Wait until the upsert is complete. The
    /// number of vectors after the upsert is
    /// returned.
    ///
    /// @param  index               io.pinecone.clients.Index
    /// @param  vectorCountExpected int
    /// @throws                     net.jmp.speeches.load.LoadException When a timeout occurs waiting for the upsert to complete
    private void waitUntilUpsertIsComplete(final Index index, final int vectorCountExpected) throws LoadException {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(index, vectorCountExpected));
        }

        int vectorCountCurrent = this.getVectorCount(index);
        int count = 0;

        while (vectorCountCurrent < vectorCountExpected && count < this.loadTimeoutInSeconds) {
            try {
                ++count;

                this.logger.warn("Waiting 1 second - iteration {} of {}", count, this.loadTimeoutInSeconds);

                Thread.sleep(1_000);

                vectorCountCurrent = this.getVectorCount(index);
            } catch (final InterruptedException ie) {
                this.logger.error(catching(ie));
                Thread.currentThread().interrupt();
            }
        }

        this.logger.info("Done waiting");

        if (count >= this.loadTimeoutInSeconds) {
            throw new LoadException("Timed out waiting for the upsert of the vectors to complete");
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Get the vector count from the index
    /// by describing the index statistics.
    ///
    /// @param  index   io.pinecone.clients.Index
    /// @return         int
    private int getVectorCount(final Index index) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(index));
        }

        int result = 0;

        final DescribeIndexStatsResponse indexStatsResponse = index.describeIndexStats();
        final Map<String, NamespaceSummary> namespaces = indexStatsResponse.getNamespacesMap();
        final NamespaceSummary namespaceSummary = namespaces.get(this.namespace);

        if (namespaceSummary != null) {
            result = namespaceSummary.getVectorCount();
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(result));
        }

        return result;
    }

    /// Insert a new vector document into MongoDb.
    ///
    /// @param  speechDocument  net.jmp.speeches.documents.MongoSpeechDocument
    /// @param  upsertRecords   java.util.List<java.util.Map<java.lang.String, java.lang.String>>
    private void insertVectorDocument(final MongoSpeechDocument speechDocument, final List<Map<String, String>> upsertRecords) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(speechDocument, upsertRecords));
        }

        final MongoVectorDocument vectorDocument = new MongoVectorDocument();

        vectorDocument.setSpeechId(speechDocument.getId());
        vectorDocument.setTitle(speechDocument.getTextAnalysis().getTitle());
        vectorDocument.setAuthor(speechDocument.getTextAnalysis().getAuthor());

        for (final Map<String, String> upsertRecord : upsertRecords) {
            vectorDocument.addVectorId(upsertRecord.get("_id"));
        }

        final MongoDatabase database = this.mongoClient.getDatabase(this.dbName);
        final MongoCollection<MongoVectorDocument> collection = database.getCollection(this.vectorsCollectionName, MongoVectorDocument.class);

        this.logger.info("Inserted vector document: {}", collection.insertOne(vectorDocument).getInsertedId().asObjectId().getValue());

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Get the text segments.
    ///
    /// @param  documentText    java.lang.String
    /// @return                 java.util.List<java.lang.String>
    private List<String> getTextSegments(final String documentText) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(documentText));
        }

        final TextSplitterResponse textSplitterResponse = TextSplitter.builder()
                .document(documentText)
                .maxTokens(this.maxTokens)
                .build()
                .split();

        final List<String> textSegments = textSplitterResponse.getTextSegments();

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(textSegments));
        }

        return textSegments;
    }

    /// Get the Mongo speech documents.
    ///
    /// @return  java.util.List<net.jmp.speeches.documents.MongoSpeechDocument>
    private List<MongoSpeechDocument> getSpeechDocuments() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        List<MongoSpeechDocument> speechDocuments;

        final MongoDatabase database = this.mongoClient.getDatabase(this.dbName);
        final MongoCollection<MongoSpeechDocument> collection = database.getCollection(this.speechesCollectionName, MongoSpeechDocument.class);

        final Bson projectionFields = Projections.fields(
                Projections.include("textAnalysis")
        );

        try (final MongoCursor<MongoSpeechDocument> cursor = collection
                .find()
                .projection(projectionFields)
                .iterator()) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("There are {} documents available", cursor.available());
            }

            speechDocuments = new ArrayList<>(cursor.available());

            while (cursor.hasNext()) {
                final MongoSpeechDocument speechDocument = cursor.next();
                final TextAnalyzerResponse textAnalysis = speechDocument.getTextAnalysis();

                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("ID    : {}", speechDocument.getId());
                    this.logger.debug("Title : {}", textAnalysis.getTitle());
                    this.logger.debug("Author: {}", textAnalysis.getAuthor());
                }

                speechDocuments.add(speechDocument);
            }
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(speechDocuments));
        }

        return speechDocuments;
    }

    /// The builder class.
    public static class Builder {
        /// The pinecone client.
        private Pinecone pinecone;
        
        /// The searchable embedding model.
        private String searchableEmbeddingModel;
        
        /// The searchable index name.
        private String searchableIndexName;
        
        /// The namespace.
        private String namespace;

        /// The mongo client.
        private MongoClient mongoClient;

        /// The speeches collection name.
        private String speechesCollectionName;

        /// The vectors collection name.
        private String vectorsCollectionName;

        /// The database name.
        private String dbName;

        /// The max tokens.
        private int maxTokens;

        /// The timeout in seconds.
        private int timeoutInSeconds;

        /// The default constructor.
        private Builder() {
            super();
        }

        /// Set the pinecone client.
        ///
        /// @param  pinecone    net.jmp.speeches.Pinecone
        /// @return             net.jmp.speeches.load.Load.Builder
        public Builder pinecone(final Pinecone pinecone) {
            this.pinecone = pinecone;

            return this;
        }
        
        /// Set the searchable embedding model.
        ///
        /// @param  searchableEmbeddingModel    java.lang.String
        /// @return                             net.jmp.speeches.load.Load.Builder
        public Builder searchableEmbeddingModel(final String searchableEmbeddingModel) {
            this.searchableEmbeddingModel = searchableEmbeddingModel;

            return this;
        }
        
        /// Set the searchable index name.
        ///
        /// @param  searchableIndexName java.lang.String
        /// @return                     net.jmp.speeches.load.Load.Builder
        public Builder searchableIndexName(final String searchableIndexName) {
            this.searchableIndexName = searchableIndexName;

            return this;
        }
        
        /// Set the namespace.
        ///
        /// @param  namespace   java.lang.String
        /// @return             net.jmp.speeches.load.Load.Builder
        public Builder namespace(final String namespace) {
            this.namespace = namespace;

            return this;
        }

        /// Set the mongo client.
        ///
        /// @param  mongoClient io.mongodb.client.MongoClient
        /// @return             net.jmp.speeches.load.Load.Builder
        public Builder mongoClient(final MongoClient mongoClient) {
            this.mongoClient = mongoClient;

            return this;
        }

        /// Set the speeches collection name.
        ///
        /// @param  speechesCollectionName  java.lang.String
        /// @return                         net.jmp.speeches.load.Load.Builder
        public Builder speechesCollectionName(final String speechesCollectionName) {
            this.speechesCollectionName = speechesCollectionName;

            return this;
        }

        /// Set the vectors collection name.
        ///
        /// @param  vectorsCollectionName   java.lang.String
        /// @return                         net.jmp.speeches.load.Load.Builder
        public Builder vectorsCollectionName(final String vectorsCollectionName) {
            this.vectorsCollectionName = vectorsCollectionName;

            return this;
        }

        /// Set the database name.
        ///
        /// @param  dbName java.lang.String
        /// @return        net.jmp.speeches.load.Load.Builder
        public Builder dbName(final String dbName) {
            this.dbName = dbName;

            return this;
        }

        /// Set the number of maximum tokens.
        ///
        /// @param  maxTokens   int
        /// @return         net.jmp.speeches.load.Load.Builder
        public Builder maxTokens(final int maxTokens) {
            this.maxTokens = maxTokens;

            return this;
        }

        /// Set the timeout in seconds.
        ///
        /// @param  timeoutInSeconds    int
        /// @return                     net.jmp.speeches.load.Load.Builder
        public Builder timeoutInSeconds(final int timeoutInSeconds) {
            this.timeoutInSeconds = timeoutInSeconds;

            return this;
        }

        /// Build the load index.
        ///
        /// @return net.jmp.speeches.load.Load
        public Load build() {
            return new Load(this);
        }
    }
}
