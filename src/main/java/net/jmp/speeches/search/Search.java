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

import io.pinecone.clients.Index;
import io.pinecone.clients.Pinecone;

import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.jmp.speeches.Operation;

import static net.jmp.util.logging.LoggerUtils.*;

import net.jmp.speeches.documents.MongoSpeechDocument;

import net.jmp.speeches.utils.MongoUtils;

import org.openapitools.db_data.client.ApiException;

import org.openapitools.db_data.client.model.Hit;
import org.openapitools.db_data.client.model.SearchRecordsResponse;
import org.openapitools.db_data.client.model.SearchRecordsResponseResult;

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

    /// The map of speech author last names to their full names.
    private final Map<String, String> authorNames = new HashMap<>();

    /// Regular expression pattern to get the last word in a string (last name).
    private final Pattern patternLastWord = Pattern.compile("(\\w+)$");

    /// The Gradle task name.
    private final String gradleTaskName;

    /// The list of fields to be searched within the records.
    private final List<String> searchFields = List.of("text_segment", "title", "author");

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

        this.gradleTaskName = builder.gradleTaskName;
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

        final Set<String> titlesInQuery = this.findTitles();
        final Set<String> authorsInQuery = this.findAuthorFullNames();

        authorsInQuery.addAll(this.findAuthorLastNames());

        final Map<String, Object> filters = new HashMap<>();

        /* Unfortunately the last title and author are included */

        if (!titlesInQuery.isEmpty() || !authorsInQuery.isEmpty()) {
            titlesInQuery.forEach(title -> filters.put("title", title));
            authorsInQuery.forEach(author -> filters.put("author", author));
        }

        /* Search by Gradle task name */

        switch (this.gradleTaskName) {
            case "search" -> this.search(filters);
            case "search-by-author-full-name" -> this.searchByAuthorFullName(filters);
            case "search-by-author-last-name" -> this.searchByAuthorLastName(filters);
            case "search-by-combo" -> this.searchByCombo(filters);
            case "search-by-title" -> this.searchByTitle(filters);
            default -> this.logger.error("Unrecognized Gradle task name: {}", this.gradleTaskName);
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Search the Pinecone index.
    ///
    /// @param  filters java.util.Map<java.lang.String, java.lang.Object>
    private void search(final Map<String, Object> filters) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(filters));
        }

        try (final Index index = this.pinecone.getIndexConnection(this.searchableIndexName)) {
            try {
                final SearchRecordsResponse response = index.searchRecordsByText(
                        this.queryText,
                        this.namespace,
                        this.searchFields,
                        this.topK,
                        filters,
                        null
                );

                final SearchRecordsResponseResult result = response.getResult();
                final List<Hit> hits = result.getHits();

                this.logger.info("Search records by text found {} hits: ", hits.size());

                for (final Hit hit : hits) {
                    this.logHit(hit);
                    this.logContent(hit);
                }
            } catch (final ApiException e) {
                this.logger.error(catching(e));
            }
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Search the Pinecone index by author full name.
    ///
    /// @param  filters java.util.Map<java.lang.String, java.lang.Object>
    private void searchByAuthorFullName(final Map<String, Object> filters) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(filters));
        }

        final String author = (String) filters.get("author");   // {author=Gerald R. Ford}

        this.logger.info("In search by author full name: {}", author);

        this.search(filters);

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Search the Pinecone index by author last name.
    ///
    /// @param  filters java.util.Map<java.lang.String, java.lang.Object>
    private void searchByAuthorLastName(final Map<String, Object> filters) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(filters));
        }

        final String author = (String) filters.get("author");   // {author=Richard M. Nixon}

        this.logger.info("In search by author last name: {}", author);

        this.search(filters);

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Search the Pinecone index by title and author.
    ///
    /// @param  filters java.util.Map<java.lang.String, java.lang.Object>
    private void searchByCombo(final Map<String, Object> filters) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(filters));
        }

        final String author = (String) filters.get("author");   // {author=Abraham Lincoln}
        final String title = (String) filters.get("title");     // {title=Second Inaugural}

        this.logger.info("In search by combo: {} by {}", title, author);

        this.search(filters);

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Search the Pinecone index by title.
    ///
    /// @param  filters java.util.Map<java.lang.String, java.lang.Object>
    private void searchByTitle(final Map<String, Object> filters) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(filters));
        }

        final String title = (String) filters.get("title");   // {title=Address to British Parliament}

        this.logger.info("In search by title: {}", title);

        this.search(filters);

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Load the speech sets.
    private void loadSpeechSets() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        final List<MongoSpeechDocument> speechDocuments = MongoUtils.getSpeechDocuments(
                this.logger,
                this.mongoClient,
                this.dbName,
                this.speechesCollectionName
        );

        this.loadSpeechTitles(speechDocuments);
        this.loadSpeechAuthors(speechDocuments);

        this.loadSpeechAuthorLastNames();

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Load the speech title.
    ///
    /// @param  speechDocuments   java.util.List<net.jmp.speeches.documents.MongoSpeechDocument>
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
    /// @param  speechDocuments   java.util.List<net.jmp.speeches.documents.MongoSpeechDocument>
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

        this.authors.forEach(author -> {
            final String authorLastName = this.getAuthorLastName(author).orElse(null);

            this.authorNames.put(authorLastName, author);
        });

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

        final Matcher matcher = this.patternLastWord.matcher(author);

        if (matcher.find()) {
            result = matcher.group(1); // Group 1 contains the captured last name
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(result));
        }

        return Optional.ofNullable(result);
    }

    /// Find the author full names in the query text.
    ///
    /// @return  java.util.Set<java.lang.String>
    private Set<String> findAuthorFullNames() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        final Set<String> results = new HashSet<>();

        this.authors.forEach(author -> {
            if (this.containsIgnoreCase(author)) {
                results.add(author);
            }
        });

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(results));
        }

        return results;
    }

    /// Find the author last names in the query text.
    ///
    /// @return  java.util.Set<java.lang.String>
    private Set<String> findAuthorLastNames() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        final Set<String> results = new HashSet<>();

        this.authorNames.keySet().forEach(lastName -> {
            if (this.containsIgnoreCase(lastName)) {
                results.add(this.authorNames.get(lastName));
            }
        });

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(results));
        }

        return results;
    }

    /// Find the speech titles in the query text.
    ///
    /// @return  java.util.Set<java.lang.String>
    private Set<String> findTitles() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        final Set<String> results = new HashSet<>();

        this.titles.forEach(title -> {
            if (this.containsIgnoreCase(title)) {
                results.add(title);
            }
        });

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(results));
        }

        return results;
    }

    /// Check if the search string is contained in the
    /// query text without any case sensitivity.
    ///
    /// @param  searchString    java.lang.String
    /// @return                 boolean
    private boolean containsIgnoreCase(final String searchString) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(searchString));
        }

        final Pattern pattern = Pattern.compile(searchString, Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(this.queryText);
        final boolean result = matcher.find();

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(result));
        }

        return result;
    }

    /// Log a hit.
    ///
    /// @param  hit org.openapitools.db_data.client.model.Hit
    private void logHit(final Hit hit) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(hit));
        }

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Score: {}", hit.getScore());
            this.logger.debug("ID   : {}", hit.getId());

            @SuppressWarnings("unchecked") final Map<String, Object> hitFields = (Map<String, Object>) hit.getFields();

            for (final Map.Entry<String, Object> entry : hitFields.entrySet()) {
                this.logger.debug("{}: {}", entry.getKey(), entry.getValue());
            }
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Log the content.
    ///
    /// @param  hit org.openapitools.db_data.client.model.Hit
    private void logContent(final Hit hit) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(hit));
        }

        @SuppressWarnings("unchecked")
        final Map<String, Object> hitFields = (Map<String, Object>) hit.getFields();
        final String content = (String) hitFields.getOrDefault("text_segment", "");
        final String title = (String) hitFields.getOrDefault("title", "");
        final String author = (String) hitFields.getOrDefault("author", "");

        this.logger.info("{} - {} by {}", content, title, author);

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
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

        /// The number of top results to return when searching.
        private int topK;

        /// The default constructor.
        public Builder() {
            super();
        }

        /// Set the Gradle task name.
        ///
        /// @param  gradleTaskName  java.lang.String
        /// @return                 net.jmp.speeches.search.Search.Builder
        public Builder gradleTaskName(final String gradleTaskName) {
            this.gradleTaskName = gradleTaskName;

            return this;
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
