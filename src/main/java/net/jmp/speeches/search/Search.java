package net.jmp.speeches.search;

/*
 * (#)Search.java   0.4.0   07/12/2025
 * (#)Search.java   0.1.0   07/05/2025
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

import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.jmp.speeches.Operation;

import static net.jmp.util.logging.LoggerUtils.*;

import net.jmp.speeches.store.MongoSpeechDocument;

import net.jmp.speeches.text.TextAnalyzerResponse;

import org.bson.conversions.Bson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The search Pinecone index class.
///
/// @version    0.4.0
/// @since      0.1.0
public final class Search extends Operation {
    /// The logger.
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /// The set of speech titles.
    private final Set<String> titles = new HashSet<>();

    /// The set of speech authors.
    private final Set<String> authors = new HashSet<>();

    /// The set of speech author last names.
    private final Set<String> authorLastNames = new HashSet<>();

    /// Regular expression pattern to get the last word in a string (last name).
    private final Pattern pattern = Pattern.compile("(\\w+)$");

    /// The constructor.
    ///
    /// @param  builder net.jmp.speeches.search.Search.Builder
    public Search(final Builder builder) {
        super(Operation.operationBuilder()
                .pinecone(builder.pinecone)
                .chatModel(builder.chatModel)
                .searchableEmbeddingModel(builder.searchableEmbeddingModel)
                .searchableIndexName(builder.searchableIndexName)
                .namespace(builder.namespace)
                .rerankingModel(builder.rerankingModel)
                .queryText(builder.queryText)
                .openAiApiKey(builder.openAiApiKey)
                .mongoClient(builder.mongoClient)
                .dbName(builder.dbName)
                .speechesCollectionName(builder.speechesCollectionName)
                .topK(builder.topK)
        );
    }

    /// Return the builder.
    ///
    /// @return net.jmp.speeches.search.Search.Builder
    public static Builder builder() {
        return new Builder();
    }

    /// The operate method.
    @Override
    public void operate() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        this.logger.info("Searching Pinecone index: {}", this.searchableIndexName);

        this.loadSpeechSets();

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Load the speech sets.
    private void loadSpeechSets() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        final List<MongoSpeechDocument> speechDocuments = this.getSpeechDocuments();

        this.loadSpeechTitles(speechDocuments);
        this.loadSpeechAuthors(speechDocuments);

        this.loadSpeechAuthorLastNames();

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Load the speech title.
    ///
    /// @param  speechDocuments   java.util.List<net.jmp.speeches.store.MongoSpeechDocument>
    private void loadSpeechTitles(final List<MongoSpeechDocument> speechDocuments) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(speechDocuments));
        }

        for (final MongoSpeechDocument speechDocument : speechDocuments) {
            this.titles.add(speechDocument.getTextAnalysis().getTitle());
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Load the speech authors.
    ///
    /// @param  speechDocuments   java.util.List<net.jmp.speeches.store.MongoSpeechDocument>
    private void loadSpeechAuthors(final List<MongoSpeechDocument> speechDocuments) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(speechDocuments));
        }

        for (final MongoSpeechDocument speechDocument : speechDocuments) {
            this.authors.add(speechDocument.getTextAnalysis().getAuthor());
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Load the speech author last names.
    private void loadSpeechAuthorLastNames() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        this.authors.forEach(author -> this.authorLastNames.add(this.getAuthorLastName(author).orElse("")));

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Get the author last name.
    ///
    /// @param  author  java.lang.String
    /// @return         java.util.Optional<java.lang.String>
    private Optional<String> getAuthorLastName(final String author) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(author));
        }

        String result = null;

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }

        final Matcher matcher = this.pattern.matcher(author);

        if (matcher.find()) {
            result = matcher.group(1); // Group 1 contains the captured last name
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(result));
        }

        return Optional.ofNullable(result);
    }

    /// Get the Mongo speech documents.
    ///
    /// @return  java.util.List<net.jmp.speeches.store.MongoSpeechDocument>
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
        /// The Pinecone client.
        private Pinecone pinecone;

        /// The chat model.
        private String chatModel;

        /// The searchable embedding model.
        private String searchableEmbeddingModel;

        /// The searchable index name.
        private String searchableIndexName;

        /// The namespace.
        private String namespace;

        /// The re-ranking model.
        private String rerankingModel;

        /// The query text.
        private String queryText;

        /// The OpenAI API key.
        private String openAiApiKey;

        /// The MongoDB client.
        private MongoClient mongoClient;

        /// The MongoDB speeches collection name.
        private String speechesCollectionName;

        /// The MongoDB database name.
        private String dbName;

        /// The number of top results to return when searching.
        private int topK;

        /// The default constructor.
        public Builder() {
            super();
        }

        /// Set the Pinecone client.
        ///
        /// @param  pinecone net.jmp.pinecone.Pinecone
        /// @return          net.jmp.speeches.search.Search.Builder
        public Builder pinecone(final Pinecone pinecone) {
            this.pinecone = pinecone;

            return this;
        }

        /// Set the chat model.
        ///
        /// @param  chatModel   java.lang.String
        /// @return             net.jmp.speeches.search.Search.Builder
        public Builder chatModel(final String chatModel) {
            this.chatModel = chatModel;

            return this;
        }

        /// Set the searchable embedding model.
        ///
        /// @param  searchableEmbeddingModel    java.lang.String
        /// @return                             net.jmp.speeches.search.Search.Builder
        public Builder searchableEmbeddingModel(final String searchableEmbeddingModel) {
            this.searchableEmbeddingModel = searchableEmbeddingModel;

            return this;
        }

        /// Set the searchable index name.
        ///
        /// @param  searchableIndexName java.lang.String
        /// @return                     net.jmp.speeches.search.Search.Builder
        public Builder searchableIndexName(final String searchableIndexName) {
            this.searchableIndexName = searchableIndexName;

            return this;
        }

        /// Set the namespace.
        ///
        /// @param  namespace java.lang.String
        /// @return           net.jmp.speeches.search.Search.Builder
        public Builder namespace(final String namespace) {
            this.namespace = namespace;

            return this;
        }

        /// Set the re-ranking model.
        ///
        /// @param  rerankingModel java.lang.String
        /// @return                net.jmp.speeches.search.Search.Builder
        public Builder rerankingModel(final String rerankingModel) {
            this.rerankingModel = rerankingModel;

            return this;
        }

        /// Set the query or search text.
        ///
        /// @param  queryText java.lang.String
        /// @return           net.jmp.speeches.search.Search.Builder
        public Builder queryText(final String queryText) {
            this.queryText = queryText;

            return this;
        }

        /// Set the OpenAI API key.
        ///
        /// @param  openAiApiKey java.lang.String
        /// @return              net.jmp.speeches.search.Search.Builder
        public Builder openAiApiKey(final String openAiApiKey) {
            this.openAiApiKey = openAiApiKey;

            return this;
        }

        /// Set the MongoDB client.
        ///
        /// @param  mongoClient com.mongodb.client.MongoClient
        /// @return             net.jmp.speeches.search.Search.Builder
        public Builder mongoClient(final MongoClient mongoClient) {
            this.mongoClient = mongoClient;

            return this;
        }

        /// Set the MongoDB collection name.
        ///
        /// @param  speechesCollectionName  java.lang.String
        /// @return                         net.jmp.speeches.search.Search.Builder
        public Builder speechesCollectionName(final String speechesCollectionName) {
            this.speechesCollectionName = speechesCollectionName;

            return this;
        }

        /// Set the MongoDB database name.
        ///
        /// @param  dbName java.lang.String
        /// @return        net.jmp.speeches.search.Search.Builder
        public Builder dbName(final String dbName) {
            this.dbName = dbName;

            return this;
        }

        /// Set the topK value.
        ///
        /// @param  topK    int
        /// @return         net.jmp.speeches.search.Search.Builder
        public Builder topK(final int topK) {
            this.topK = topK;

            return this;
        }

        /// Build the search object.
        ///
        /// @return  net.jmp.speeches.search.Search
        public Search build() {
            return new Search(this);
        }
    }
}
