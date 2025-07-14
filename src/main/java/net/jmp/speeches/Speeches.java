package net.jmp.speeches;

/*
 * (#)Speeches.java 0.4.0   07/12/2025
 * (#)Speeches.java 0.3.0   07/09/2025
 * (#)Speeches.java 0.2.0   07/08/2025
 * (#)Speeches.java 0.1.0   07/05/2025
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

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import io.pinecone.clients.Pinecone;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;

import java.util.*;

import static net.jmp.util.logging.LoggerUtils.*;

import net.jmp.speeches.create.Create;
import net.jmp.speeches.delete.Delete;
import net.jmp.speeches.load.Load;
import net.jmp.speeches.search.Search;
import net.jmp.speeches.store.Store;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

import org.bson.codecs.configuration.CodecRegistry;

import org.bson.codecs.pojo.PojoCodecProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The speeches class.
///
/// @version    0.4.0
/// @since      0.1.0
final class Speeches {
    /// The logger.
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /// The chat model.
    private final String chatModel;

    /// The searchable embedding model.
    private final String searchableEmbeddingModel;

    /// The searchable index name.
    private final String searchableIndexName;

    /// The MongoDB speeches collection.
    private final String mongoDbCollectionSpeeches;

    /// The MongoDB vectors collection.
    private final String mongoDbCollectionVectors;

    /// The MongoDB name.
    private final String mongoDbName;

    /// The MongoDB URI file name.
    private final String mongoDbUriFile;

    /// The namespace.
    private final String namespace;

    /// The reranking model.
    private final String rerankingModel;

    /// The query text.
    private final String queryText;

    /// The Open AI API key.
    private String openAiApiKey;

    /// The location of the speeches.
    private final String speechesLocation;

    /// The number of top results to return when querying.
    private final int topK;

    /// The maximum number of tokens when embedding.
    private final int maxTokens;

    /// The load timeout in seconds.
    private final int loadTimeoutInSeconds;

    /// The constructor.
    ///
    /// @param  builder net.jmp.speeches.Speeches.Builder
    private Speeches(final Builder builder) {
        super();

        this.chatModel = builder.chatModel;
        this.searchableEmbeddingModel = builder.searchableEmbeddingModel;
        this.searchableIndexName = builder.searchableIndexName;
        this.mongoDbCollectionSpeeches = builder.mongoDbCollectionSpeeches;
        this.mongoDbCollectionVectors = builder.mongoDbCollectionVectors;
        this.mongoDbName = builder.mongoDbName;
        this.mongoDbUriFile = builder.mongoDbUriFile;
        this.namespace = builder.namespace;
        this.rerankingModel = builder.rerankingModel;
        this.queryText = builder.queryText;
        this.speechesLocation = builder.speechesLocation;
        this.topK = builder.topK;
        this.maxTokens = builder.maxTokens;
        this.loadTimeoutInSeconds = builder.loadTimeoutInSeconds;
    }

    /// The builder method.
    ///
    /// @return net.jmp.speeches.Speeches.Builder
    static Builder builder() {
        return new Builder();
    }

    /// The start method.
    ///
    /// @param  operation   java.lang.String
    void start(final String operation) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(operation));
        }

        this.openAiApiKey = this.getOpenAIApiKey().orElseThrow(() -> new RuntimeException("OpenAI API key not found"));

        final String pineconeApiKey = this.getPineconeApiKey().orElseThrow(() -> new RuntimeException("Pinecone API key not found"));
        final Pinecone pinecone = new Pinecone.Builder(pineconeApiKey).build();

        try (final MongoClient mongoClient = MongoClients.create(this.getMongoDbSettings())) {
            switch (operation) {
                case "create" -> this.create(pinecone);
                case "delete" -> this.delete(pinecone);
                case "load" -> this.load(pinecone, mongoClient);
                case "search" -> this.search(pinecone, mongoClient);
                case "store" -> this.store(mongoClient);
                default -> this.logger.error("Unknown operation: {}", operation);
            }
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Get the MongoDB settings.
    ///
    /// @return com.mongodb.MongoClientSettings
    private MongoClientSettings getMongoDbSettings() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        final String mongoDbUri = this.getMongoDbUri().orElseThrow(() -> new RuntimeException("MongoDB URI not found"));
        final ConnectionString connectionString = new ConnectionString(mongoDbUri);

        final CodecRegistry pojoCodecRegistry = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );

        final MongoClientSettings mongoDbSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .codecRegistry(pojoCodecRegistry)
                .build();

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(mongoDbSettings));
        }

        return mongoDbSettings;
    }

    /// Create the Pinecone index.
    ///
    /// @param  pinecone io.pinecone.clients.Pinecone
    private void create(final Pinecone pinecone) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(pinecone));
        }

        final Create create = Create.builder()
                .searchableEmbeddingModel(this.searchableEmbeddingModel)
                .searchableIndexName(this.searchableIndexName)
                .pinecone(pinecone)
                .build();

        create.operate();

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Delete the Pinecone index.
    ///
    /// @param  pinecone io.pinecone.clients.Pinecone
    private void delete(final Pinecone pinecone) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(pinecone));
        }

        final Delete delete = Delete.builder()
                .searchableIndexName(this.searchableIndexName)
                .pinecone(pinecone)
                .build();

        delete.operate();

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Load the speeches from MongoDB into Pinecone.
    ///
    /// @param  pinecone io.pinecone.clients.Pinecone
    /// @param  mongoClient org.mongodb.mongo.MongoClient
    private void load(final Pinecone pinecone, final MongoClient mongoClient) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(pinecone, mongoClient));
        }

        final Load load = Load.builder()
                .searchableEmbeddingModel(this.searchableEmbeddingModel)
                .searchableIndexName(this.searchableIndexName)
                .namespace(this.namespace)
                .pinecone(pinecone)
                .mongoClient(mongoClient)
                .speechesCollectionName(this.mongoDbCollectionSpeeches)
                .vectorsCollectionName(this.mongoDbCollectionVectors)
                .dbName(this.mongoDbName)
                .maxTokens(this.maxTokens)
                .timeoutInSeconds(this.loadTimeoutInSeconds)
                .build();

        load.operate();

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Search the Pinecone index.
    ///
    /// @param  pinecone    io.pinecone.clients.Pinecone
    /// @param  mongoClient org.mongodb.mongo.MongoClient
    private void search(final Pinecone pinecone, final MongoClient mongoClient) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(pinecone, mongoClient));
        }

        final Search search = Search.builder()
                .searchableIndexName(this.searchableIndexName)
                .searchableEmbeddingModel(this.searchableEmbeddingModel)
                .pinecone(pinecone)
                .chatModel(this.chatModel)
                .namespace(this.namespace)
                .rerankingModel(this.rerankingModel)
                .queryText(this.queryText)
                .openAiApiKey(this.openAiApiKey)
                .mongoClient(mongoClient)
                .speechesCollectionName(this.mongoDbCollectionSpeeches)
                .dbName(this.mongoDbName)
                .topK(this.topK)
                .build();

        search.operate();

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Store the speeches into MongoDB.
    ///
    /// @param  mongoClient org.mongodb.mongo.MongoClient
    private void store(final MongoClient mongoClient) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(mongoClient));
        }

        final Store store = Store.builder()
                .mongoClient(mongoClient)
                .speechesCollectionName(this.mongoDbCollectionSpeeches)
                .dbName(this.mongoDbName)
                .speechesLocation(this.speechesLocation)
                .build();

        store.operate();

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Get the OpenAI API key.
    ///
    /// @return java.util.Optional<java.lang.String>
    private Optional<String> getOpenAIApiKey() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        final Optional<String> apiKey = this.getApiKey("app.openaiApiKey");

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(apiKey));
        }

        return apiKey;
    }

    /// Get the Pinecone API key.
    ///
    /// @return java.util.Optional<java.lang.String>
    private Optional<String> getPineconeApiKey() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        final Optional<String> apiKey = this.getApiKey("app.pineconeApiKey");

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(apiKey));
        }

        return apiKey;
    }

    /// Get the API key.
    ///
    /// @param  propertyName    java.lang.String
    /// @return                 java.util.Optional<java.lang.String>
    private Optional<String> getApiKey(final String propertyName) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(propertyName));
        }

        final String apiKeyFileName = System.getProperty(propertyName);

        String apiKey = null;

        try {
            apiKey = Files.readString(Paths.get(apiKeyFileName)).trim();

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("API key file: {}", apiKeyFileName);
                this.logger.debug("API key: {}", apiKey);
            }
        } catch (final IOException ioe) {
            this.logger.error("Unable to read API key file: {}", apiKeyFileName, ioe);
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(apiKey));
        }

        return Optional.ofNullable(apiKey);
    }

    /// Get the MongoDB URI.
    ///
    /// @return java.util.Optional<java.lang.String>
    private Optional<String> getMongoDbUri() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        final String mongoDbUriFileName = this.mongoDbUriFile;

        String mongoDbUri = null;

        try {
            mongoDbUri = Files.readString(Paths.get(mongoDbUriFileName)).trim();

            if (this.logger.isDebugEnabled()) {
                this.logger.debug("MongoDb URI file: {}", mongoDbUriFileName);
                this.logger.debug("MongoDb URI: {}", mongoDbUri);
            }
        } catch (final IOException ioe) {
            this.logger.error("Unable to read MongoDb URI file: {}", mongoDbUriFileName, ioe);
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(mongoDbUri));
        }

        return Optional.ofNullable(mongoDbUri);
    }

    /// The builder class.
    static class Builder {
        /// The chat model.
        private String chatModel;

        /// The searchable embedding model.
        private String searchableEmbeddingModel;

        /// The searchable index name.
        private String searchableIndexName;

        /// The MongoDb speeches collection.
        private String mongoDbCollectionSpeeches;

        /// The MongoDb vectors collection.
        private String mongoDbCollectionVectors;

        /// The MongoDb name.
        private String mongoDbName;

        /// The MongoDb URI file name.
        private String mongoDbUriFile;

        /// The namespace.
        private String namespace;

        /// The reranking model.
        private String rerankingModel;

        /// The query text.
        private String queryText;

        /// The speeches location.
        private String speechesLocation;

        /// The number of top results to return when querying.
        private int topK;

        /// The maximum number of tokens when embedding.
        private int maxTokens;

        /// The load timeout in seconds.
        private int loadTimeoutInSeconds;

        /// The default constructor.
        Builder() {
            super();
        }

        /// Set the chat model.
        ///
        /// @param  chatModel   java.lang.String
        /// @return             net.jmp.speeches.Speeches.Builder
        public Builder chatModel(final String chatModel) {
            this.chatModel = chatModel;

            return this;
        }

        /// Set the searchable embedding model.
        ///
        /// @param  searchableEmbeddingModel    java.lang.String
        /// @return                             net.jmp.speeches.Speeches.Builder
        public Builder searchableEmbeddingModel(final String searchableEmbeddingModel) {
            this.searchableEmbeddingModel = searchableEmbeddingModel;

            return this;
        }

        /// Set the searchable index name.
        ///
        /// @param  searchableIndexName java.lang.String
        /// @return                     net.jmp.speeches.Speeches.Builder
        public Builder searchableIndexName(final String searchableIndexName) {
            this.searchableIndexName = searchableIndexName;

            return this;
        }
        
        /// Set the MongoDb speeches collection.
        ///
        /// @param  mongoDbCollectionSpeeches   java.lang.String
        /// @return                             net.jmp.speeches.Speeches.Builder
        public Builder mongoDbCollectionSpeeches(final String mongoDbCollectionSpeeches) {
            this.mongoDbCollectionSpeeches = mongoDbCollectionSpeeches;

            return this;
        }

        /// Set the MongoDb vectors collection.
        ///
        /// @param  mongoDbCollectionVectors    java.lang.String
        /// @return                             net.jmp.speeches.Speeches.Builder
        public Builder mongoDbCollectionVectors(final String mongoDbCollectionVectors) {
            this.mongoDbCollectionVectors = mongoDbCollectionVectors;

            return this;
        }

        /// Set the MongoDb name.
        ///
        /// @param  mongoDbName java.lang.String
        /// @return             net.jmp.speeches.Speeches.Builder
        public Builder mongoDbName(final String mongoDbName) {
            this.mongoDbName = mongoDbName;

            return this;
        }

        /// Set the MongoDb URI.
        ///
        /// @param  mongoDbUriFile  java.lang.String
        /// @return                 net.jmp.speeches.Speeches.Builder
        public Builder mongoDbUriFile(final String mongoDbUriFile) {
            this.mongoDbUriFile = mongoDbUriFile;

            return this;
        }

        /// Set the namespace.
        ///
        /// @param  namespace   java.lang.String
        /// @return             net.jmp.speeches.Speeches.Builder
        public Builder namespace(final String namespace) {
            this.namespace = namespace;

            return this;
        }

        /// Set the reranking model.
        ///
        /// @param  rerankingModel  java.lang.String
        /// @return                 net.jmp.speeches.Speeches.Builder
        public Builder rerankingModel(final String rerankingModel) {
            this.rerankingModel = rerankingModel;

            return this;
        }

        /// Set the query text.
        ///
        /// @param  queryText   java.lang.String
        /// @return             net.jmp.speeches.Speeches.Builder
        public Builder queryText(final String queryText) {
            this.queryText = queryText;

            return this;
        }

        /// Set the speeches location.
        ///
        /// @param  speechesLocation    java.lang.String
        /// @return                     net.jmp.speeches.Speeches.Builder
        public Builder speechesLocation(final String speechesLocation) {
            this.speechesLocation = speechesLocation;

            return this;
        }

        /// Set the topK value.
        ///
        /// @param  topK    int
        /// @return         net.jmp.speeches.Speeches.Builder
        public Builder topK(final int topK) {
            this.topK = topK;

            return this;
        }

        /// Set the maximum number of tokens.
        ///
        /// @param  maxTokens   int
        /// @return             net.jmp.speeches.Speeches.Builder
        public Builder maxTokens(final int maxTokens) {
            this.maxTokens = maxTokens;

            return this;
        }

        /// Set the load timeout in seconds.
        ///
        /// @param  loadTimeoutInSeconds    int
        /// @return                         net.jmp.speeches.Speeches.Builder
        public Builder loadTimeoutInSeconds(final int loadTimeoutInSeconds) {
            this.loadTimeoutInSeconds = loadTimeoutInSeconds;

            return this;
        }

        /// Build the speeches object.
        ///
        /// @return net.jmp.speeches.Speeches
        public Speeches build() {
            return new Speeches(this);
        }
    }
}
