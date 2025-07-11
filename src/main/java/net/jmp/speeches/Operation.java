package net.jmp.speeches;

/*
 * (#)Operation.java    0.3.0   07/10/2025
 * (#)Operation.java    0.1.0   07/05/2025
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

import io.pinecone.clients.Index;
import io.pinecone.clients.Pinecone;

import io.pinecone.proto.ListResponse;

import java.util.List;

import static net.jmp.util.logging.LoggerUtils.*;

import org.openapitools.db_control.client.model.IndexList;
import org.openapitools.db_control.client.model.IndexModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

///
/// The abstract operation class.
///
/// @version    0.3.0
/// @since      0.1.0
public abstract class Operation {
    /// The logger.
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /// The Pinecone client.
    protected final Pinecone pinecone;

    /// The chat model.
    protected final String chatModel;

    /// The searchable embedding model.
    protected final String searchableEmbeddingModel;

    /// The searchable index name.
    protected final String searchableIndexName;

    /// The namespace.
    protected final String namespace;

    /// The reranking model.
    protected final String rerankingModel;

    /// The query text.
    protected final String queryText;

    /// The OpenAI API key.
    protected final String openAiApiKey;

    /// The MongoDB client.
    protected final MongoClient mongoClient;

    /// The MongoDB speeches collection name.
    protected final String speechesCollectionName;

    /// The MongoDB vectors collection name.
    protected final String vectorsCollectionName;

    /// The MongoDB database name.
    protected final String dbName;

    /// The location of the speeches.
    protected final String speechesLocation;

    /// The number of top results to return when querying.
    protected final int topK;

    /// The maximum number of tokens when embedding.
    protected final int maxTokens;

    /// The load timeout in seconds.
    protected final int loadTimeoutInSeconds;

    /// The constructor.
    ///
    /// @param operationBuilder net.jmp.speeches.Operation.OperationBuilder
    protected Operation(final OperationBuilder operationBuilder) {
        super();

        this.pinecone = operationBuilder.pinecone;
        this.chatModel = operationBuilder.chatModel;
        this.searchableEmbeddingModel = operationBuilder.searchableEmbeddingModel;
        this.searchableIndexName = operationBuilder.searchableIndexName;
        this.namespace = operationBuilder.namespace;
        this.rerankingModel = operationBuilder.rerankingModel;
        this.queryText = operationBuilder.queryText;
        this.openAiApiKey = operationBuilder.openAiApiKey;
        this.mongoClient = operationBuilder.mongoClient;
        this.speechesCollectionName = operationBuilder.speechesCollectionName;
        this.vectorsCollectionName = operationBuilder.vectorsCollectionName;
        this.dbName = operationBuilder.dbName;
        this.speechesLocation = operationBuilder.speechesLocation;
        this.topK = operationBuilder.topK;
        this.maxTokens = operationBuilder.maxTokens;
        this.loadTimeoutInSeconds = operationBuilder.loadTimeoutInSeconds;
    }

    /// Return the operation builder.
    ///
    /// @return net.jmp.speeches.Operation.OperationBuilder
    protected static OperationBuilder operationBuilder() {
        return new OperationBuilder();
    }

    /// The operate method.
    public abstract void operate();

    /// Return true if the searchable index exists.
    ///
    /// @return boolean
    protected boolean doesSearchableIndexExist() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        final boolean result = this.doesIndexExist(this.searchableIndexName);

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(result));
        }

        return result;
    }

    /// Check if the named index exists.
    ///
    /// @param  indexName   java.lang.String
    /// @return             boolean
    private boolean doesIndexExist(final String indexName) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(indexName));
        }

        boolean result = false;

        final IndexList indexList = this.pinecone.listIndexes();
        final List<IndexModel> indexes = indexList.getIndexes();

        if (indexes != null) {
            for (final IndexModel indexModel : indexes) {
                if (indexModel.getName().equals(indexName)) {
                    result = true;

                    break;
                }
            }
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(result));
        }

        return result;
    }

    /// Check if the searchable index is loaded.
    ///
    /// @return boolean
    protected boolean isSearchableIndexLoaded() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        final boolean result = this.isNamedIndexLoaded(this.searchableIndexName, this.namespace);

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(result));
        }

        return result;
    }

    /// Check if the named index is loaded.
    ///
    /// @param  indexName   java.lang.String
    /// @param  namespace   java.lang.String
    /// @return             boolean
    private boolean isNamedIndexLoaded(final String indexName, final String namespace) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(indexName, namespace));
        }

        int vectorsCount = 0;

        try (final Index index = this.pinecone.getIndexConnection(indexName)) {
            final ListResponse response = index.list(namespace);

            vectorsCount = response.getVectorsCount();
        }

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Index name   : {}", indexName);
            this.logger.debug("Namespace    : {}", namespace);
            this.logger.debug("Vectors count: {}", vectorsCount);
        }

        final boolean result = vectorsCount > 0;

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(result));
        }

        return result;
    }

    /// The operation builder class.
    protected static class OperationBuilder {
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

        /// The reranking model.
        private String rerankingModel;

        /// The query text.
        private String queryText;

        /// The OpenAI API key.
        private String openAiApiKey;

        /// The MongoDB client.
        private MongoClient mongoClient;

        /// The MongoDB speeches collection name.
        private String speechesCollectionName;

        /// The MongoDB vectors collection name.
        private String vectorsCollectionName;

        /// The MongoDB database name.
        private String dbName;

        /// The location of the speeches.
        private String speechesLocation;

        /// The number of top results to return when querying.
        private int topK;

        /// The maximum number of tokens when embedding.
        private int maxTokens;

        /// The load timeout in seconds.
        private int loadTimeoutInSeconds;

        /// The default constructor.
        protected OperationBuilder() {
            super();
        }

        /// Set the Pinecone client.
        ///
        /// @param  pinecone net.jmp.pinecone.Pinecone
        /// @return          net.jmp.speeches.Operation.OperationBuilder
        public OperationBuilder pinecone(final Pinecone pinecone) {
            this.pinecone = pinecone;

            return this;
        }

        /// Set the chat model.
        ///
        /// @param  chatModel   java.lang.String
        /// @return             net.jmp.speeches.Operation.OperationBuilder
        public OperationBuilder chatModel(final String chatModel) {
            this.chatModel = chatModel;

            return this;
        }

        /// Set the searchable embedding model.
        ///
        /// @param  searchableEmbeddingModel    java.lang.String
        /// @return                             net.jmp.speeches.Operation.OperationBuilder
        public OperationBuilder searchableEmbeddingModel(final String searchableEmbeddingModel) {
            this.searchableEmbeddingModel = searchableEmbeddingModel;

            return this;
        }

        /// Set the searchable index name.
        ///
        /// @param  searchableIndexName java.lang.String
        /// @return                     net.jmp.speeches.Operation.OperationBuilder
        public OperationBuilder searchableIndexName(final String searchableIndexName) {
            this.searchableIndexName = searchableIndexName;

            return this;
        }

        /// Set the namespace.
        ///
        /// @param  namespace java.lang.String
        /// @return           net.jmp.speeches.Operation.OperationBuilder
        public OperationBuilder namespace(final String namespace) {
            this.namespace = namespace;

            return this;
        }

        /// Set the reranking model.
        ///
        /// @param  rerankingModel java.lang.String
        /// @return                net.jmp.speeches.Operation.OperationBuilder
        public OperationBuilder rerankingModel(final String rerankingModel) {
            this.rerankingModel = rerankingModel;

            return this;
        }

        /// Set the query text.
        ///
        /// @param  queryText java.lang.String
        /// @return           net.jmp.speeches.Operation.OperationBuilder
        public OperationBuilder queryText(final String queryText) {
            this.queryText = queryText;

            return this;
        }

        /// Set the OpenAI API key.
        ///
        /// @param  openAiApiKey java.lang.String
        /// @return              net.jmp.speeches.Operation.OperationBuilder
        public OperationBuilder openAiApiKey(final String openAiApiKey) {
            this.openAiApiKey = openAiApiKey;

            return this;
        }

        /// Set the MongoDB client.
        ///
        /// @param  mongoClient com.mongodb.client.MongoClient
        /// @return             net.jmp.speeches.Operation.OperationBuilder
        public OperationBuilder mongoClient(final MongoClient mongoClient) {
            this.mongoClient = mongoClient;

            return this;
        }

        /// Set the MongoDB speeches collection name.
        ///
        /// @param  speechesCollectionName  java.lang.String
        /// @return                         net.jmp.speeches.Operation.OperationBuilder
        public OperationBuilder speechesCollectionName(final String speechesCollectionName) {
            this.speechesCollectionName = speechesCollectionName;

            return this;
        }

        /// Set the MongoDB vectors collection name.
        ///
        /// @param  vectorsCollectionName   java.lang.String
        /// @return                         net.jmp.speeches.Operation.OperationBuilder
        public OperationBuilder vectorsCollectionName(final String vectorsCollectionName) {
            this.vectorsCollectionName = vectorsCollectionName;

            return this;
        }

        /// Set the MongoDB database name.
        ///
        /// @param  dbName java.lang.String
        /// @return        net.jmp.speeches.Operation.OperationBuilder
        public OperationBuilder dbName(final String dbName) {
            this.dbName = dbName;

            return this;
        }

        /// Set the location of the speeches.
        ///
        /// @param  speechesLocation    java.lang.String
        /// @return                     net.jmp.speeches.Operation.OperationBuilder
        public OperationBuilder speechesLocation(final String speechesLocation) {
            this.speechesLocation = speechesLocation;

            return this;
        }

        /// Set the topK value.
        ///
        /// @param  topK    int
        /// @return         net.jmp.speeches.Operation.OperationBuilder
        public OperationBuilder topK(final int topK) {
            this.topK = topK;

            return this;
        }

        /// Set the maximum number of tokens.
        ///
        /// @param  maxTokens   int
        /// @return             net.jmp.speeches.Operation.OperationBuilder
        public OperationBuilder maxTokens(final int maxTokens) {
            this.maxTokens = maxTokens;

            return this;
        }

        /// Set the load timeout in seconds.
        ///
        /// @param  loadTimeoutInSeconds    int
        /// @return                         net.jmp.speeches.Operation.OperationBuilder
        public OperationBuilder loadTimeoutInSeconds(final int loadTimeoutInSeconds) {
            this.loadTimeoutInSeconds = loadTimeoutInSeconds;

            return this;
        }
    }
}
