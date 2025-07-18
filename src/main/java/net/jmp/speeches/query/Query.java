package net.jmp.speeches.query;

/*
 * (#)Query.java    0.4.0   07/16/2025
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

import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import com.mongodb.client.MongoClient;

import io.pinecone.clients.Pinecone;

import java.util.Optional;
import java.util.Set;

import net.jmp.speeches.Operation;

import net.jmp.speeches.utils.MetadataUtil;

import static net.jmp.util.logging.LoggerUtils.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The query Pinecone index class.
///
/// @version    0.4.0
/// @since      0.4.0
public final class Query extends Operation {
    /// The logger.
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /// The Gradle task name.
    private final String gradleTaskName;

    /// The default constructor.
    ///
    /// @param  builder net.jmp.speeches.query.Query.Builder
    public Query(final Builder builder) {
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

        this.gradleTaskName = builder.gradleTaskName;
    }

    /// Return the builder.
    ///
    /// @return net.jmp.speeches.query.Query.Builder
    public static Builder builder() {
        return new Builder();
    }

    /// The operate method.
    @Override
    public void operate() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        this.logger.info("Querying Pinecone index: {}", this.searchableIndexName);

        final Optional<Struct> optionalFilter = this.getFilter();

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Get the filter.
    ///
    /// @return java.util.Optional<com.google.protobuf.Struct>
    private Optional<Struct> getFilter() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        final MetadataUtil metadataUtil = new MetadataUtil(
                this.mongoClient,
                this.dbName,
                this.speechesCollectionName
        );

        final Set<String> titlesInQuery = metadataUtil.getTitles(this.queryText);
        final Set<String> authorsInQuery = metadataUtil.getAuthors(this.queryText);

        Struct filter = null;

        if (!titlesInQuery.isEmpty() || !authorsInQuery.isEmpty()) {
            filter = this.buildFilter(titlesInQuery, authorsInQuery);
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(filter));
        }

        return Optional.ofNullable(filter);
    }

    /// Build the filter from titles and authors
    /// found in the query text.
    ///
    /// @param   titles  java.util.Set<java.lang.String>
    /// @param   authors java.util.Set<java.lang.String>
    /// @return          com.google.protobuf.Struct
    private Struct buildFilter(final Set<String> titles, final Set<String> authors) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(titles, authors));
        }

        Struct titleFilter = null;
        Struct authorFilter = null;

        if (!titles.isEmpty()) {
            titleFilter = this.buildTitleFilter(titles);
        }

        if (!authors.isEmpty()) {
            authorFilter = this.buildAuthorFilter(authors);
        }

        Struct filter = null;

        if (titleFilter != null && authorFilter != null) {
            final ListValue.Builder listValueBuilder = ListValue.newBuilder();

            listValueBuilder.addValues(Value.newBuilder().setStructValue(titleFilter).build());
            listValueBuilder.addValues(Value.newBuilder().setStructValue(authorFilter).build());

            final ListValue valuesList = listValueBuilder.build();

            filter = Struct.newBuilder()
                    .putFields("$and", Value.newBuilder().setListValue(valuesList).build())
                    .build();
        } else {
            if (titleFilter != null) {
                filter = titleFilter;
            } else if (authorFilter != null) {
                filter = authorFilter;
            }
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(filter));
        }

        return filter;
    }

    /// Build the title filter.
    ///
    /// @param   titles  java.util.Set<java.lang.String>
    /// @return          com.google.protobuf.Struct
    private Struct buildTitleFilter(final Set<String> titles) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(titles));
        }

        final Struct filter = this.buildKeyValuesFilter("title", titles);

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(filter));
        }

        return filter;
    }

    /// Build the author filter.
    ///
    /// @param   authors java.util.Set<java.lang.String>
    /// @return         com.google.protobuf.Struct
    private Struct buildAuthorFilter(final Set<String> authors) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(authors));
        }

        final Struct filter = this.buildKeyValuesFilter("author", authors);

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(filter));
        }

        return filter;
    }

    /// Build the filter for the given key and values.
    ///
    /// @param   key    java.lang.String
    /// @param   values java.util.Set<java.lang.String>
    /// @return         com.google.protobuf.Struct
    private Struct buildKeyValuesFilter(final String key, final Set<String> values) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(key, values));
        }

        Struct filter = null;

        if (values.size() == 1) {
            filter = Struct.newBuilder()
                    .putFields(key, Value.newBuilder().setStringValue(values.iterator().next()).build())
                    .build();
        } else {
            final ListValue.Builder listValueBuilder = ListValue.newBuilder();

            for (final String value : values) {
                listValueBuilder.addValues(Value.newBuilder().setStringValue(value).build());
            }

            final ListValue valuesList = listValueBuilder.build();

            final Struct inStruct = Struct.newBuilder()
                    .putFields("$in", Value.newBuilder().setListValue(valuesList).build())
                    .build();

            filter = Struct.newBuilder()
                    .putFields(key, Value.newBuilder().setStructValue(inStruct).build())
                    .build();
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(filter));
        }

        return filter;
    }

    /// The builder class.
    public static class Builder {
        /// The Gradle task name.
        private String gradleTaskName;

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

        /// The number of top results to return when querying.
        private int topK;

        /// The default constructor.
        public Builder() {
            super();
        }

        /// Set the Gradle task name.
        ///
        /// @param  gradleTaskName  java.lang.String
        /// @return                 net.jmp.speeches.query.Query.Builder
        public Builder gradleTaskName(final String gradleTaskName) {
            this.gradleTaskName = gradleTaskName;

            return this;
        }

        /// Set the Pinecone client.
        ///
        /// @param  pinecone net.jmp.pinecone.Pinecone
        /// @return          net.jmp.speeches.query.Query.Builder
        public Builder pinecone(final Pinecone pinecone) {
            this.pinecone = pinecone;

            return this;
        }

        /// Set the chat model.
        ///
        /// @param  chatModel   java.lang.String
        /// @return             net.jmp.speeches.query.Query.Builder
        public Builder chatModel(final String chatModel) {
            this.chatModel = chatModel;

            return this;
        }

        /// Set the searchable embedding model.
        ///
        /// @param  searchableEmbeddingModel    java.lang.String
        /// @return                             net.jmp.speeches.query.Query.Builder
        public Builder searchableEmbeddingModel(final String searchableEmbeddingModel) {
            this.searchableEmbeddingModel = searchableEmbeddingModel;

            return this;
        }

        /// Set the searchable index name.
        ///
        /// @param  searchableIndexName java.lang.String
        /// @return                     net.jmp.speeches.query.Query.Builder
        public Builder searchableIndexName(final String searchableIndexName) {
            this.searchableIndexName = searchableIndexName;

            return this;
        }

        /// Set the namespace.
        ///
        /// @param  namespace java.lang.String
        /// @return           net.jmp.speeches.query.Query.Builder
        public Builder namespace(final String namespace) {
            this.namespace = namespace;

            return this;
        }

        /// Set the re-ranking model.
        ///
        /// @param  rerankingModel java.lang.String
        /// @return                net.jmp.speeches.query.Query.Builder
        public Builder rerankingModel(final String rerankingModel) {
            this.rerankingModel = rerankingModel;

            return this;
        }

        /// Set the query or search text.
        ///
        /// @param  queryText java.lang.String
        /// @return           net.jmp.speeches.query.Query.Builder
        public Builder queryText(final String queryText) {
            this.queryText = queryText;

            return this;
        }

        /// Set the OpenAI API key.
        ///
        /// @param  openAiApiKey java.lang.String
        /// @return              net.jmp.speeches.query.Query.Builder
        public Builder openAiApiKey(final String openAiApiKey) {
            this.openAiApiKey = openAiApiKey;

            return this;
        }

        /// Set the MongoDB client.
        ///
        /// @param  mongoClient com.mongodb.client.MongoClient
        /// @return             net.jmp.speeches.query.Query.Builder
        public Builder mongoClient(final MongoClient mongoClient) {
            this.mongoClient = mongoClient;

            return this;
        }

        /// Set the MongoDB collection name.
        ///
        /// @param  speechesCollectionName  java.lang.String
        /// @return                         net.jmp.speeches.query.Query.Builder
        public Builder speechesCollectionName(final String speechesCollectionName) {
            this.speechesCollectionName = speechesCollectionName;

            return this;
        }

        /// Set the MongoDB database name.
        ///
        /// @param  dbName java.lang.String
        /// @return        net.jmp.speeches.query.Query.Builder
        public Builder dbName(final String dbName) {
            this.dbName = dbName;

            return this;
        }

        /// Set the topK value.
        ///
        /// @param  topK    int
        /// @return         net.jmp.speeches.query.Query.Builder
        public Builder topK(final int topK) {
            this.topK = topK;

            return this;
        }

        /// Build the query object.
        ///
        /// @return  net.jmp.speeches.query.Query
        public Query build() {
            return new Query(this);
        }
    }
}
