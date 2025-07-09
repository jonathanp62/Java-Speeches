package net.jmp.speeches.load;

/*
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

import io.pinecone.clients.Pinecone;

import java.util.List;

import net.jmp.speeches.Operation;

import net.jmp.speeches.store.MongoDocument;

import net.jmp.speeches.text.TextAnalyzerResponse;
import net.jmp.speeches.text.TextSplitter;
import net.jmp.speeches.text.TextSplitterResponse;

import static net.jmp.util.logging.LoggerUtils.*;

import org.bson.conversions.Bson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The load Pinecone index from MongoDB class.
///
/// @version    0.3.0
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
                .collectionName(builder.collectionName)
                .dbName(builder.dbName)
                .maxTokens(builder.maxTokens)
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
        }

        final List<MongoDocument> documents = this.getMongoDocuments();

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Documents fetched: {}", documents.size());
        }

        int totalTextSegments = 0;

        for (final MongoDocument document : documents) {
            final List<String> textSegments = this.getTextSegments(document);

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Text segments: {}", textSegments.size());
            }

            totalTextSegments += textSegments.size();
        }

        this.logger.info("Total text segments: {}", totalTextSegments);

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Get the text segments.
    ///
    /// @param  document    net.jmp.speeches.store.MongoDocument
    /// @return             java.util.List<java.lang.String>
    private List<String> getTextSegments(final MongoDocument document) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(document));
        }

        final TextSplitterResponse textSplitterResponse = TextSplitter.builder()
                .document(document.getTextAnalysis().getText())
                .maxTokens(this.maxTokens)
                .build()
                .split();

        final List<String> textSegments = textSplitterResponse.getTextSegments();

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(textSegments));
        }

        return textSegments;
    }

    /// Get the Mongo documents.
    ///
    /// @return  java.util.List<net.jmp.speeches.store.MongoDocument>
    private List<MongoDocument> getMongoDocuments() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        final List<MongoDocument> documents = new java.util.ArrayList<>();

        final MongoDatabase database = this.mongoClient.getDatabase(this.dbName);
        final MongoCollection<MongoDocument> collection = database.getCollection(this.collectionName, MongoDocument.class);

        final Bson projectionFields = Projections.fields(
                Projections.include("textAnalysis")
        );

        try (final MongoCursor<MongoDocument> cursor = collection
                .find()
                .projection(projectionFields)
                .iterator()) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("There are {} documents available", cursor.available());
            }

            while (cursor.hasNext()) {
                final MongoDocument document = cursor.next();
                final TextAnalyzerResponse textAnalysis = document.getTextAnalysis();

                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Title : {}", textAnalysis.getTitle());
                    this.logger.debug("Author: {}", textAnalysis.getAuthor());
                }

                documents.add(document);
            }
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(documents));
        }

        return documents;
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

        /// The collection name.
        private String collectionName;

        /// The database name.
        private String dbName;

        /// The max tokens.
        private int maxTokens;

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

        /// Set the collection name.
        ///
        /// @param  collectionName java.lang.String
        /// @return                net.jmp.speeches.load.Load.Builder
        public Builder collectionName(final String collectionName) {
            this.collectionName = collectionName;

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

        /// Build the load index.
        ///
        /// @return net.jmp.speeches.load.Load
        public Load build() {
            return new Load(this);
        }
    }
}
