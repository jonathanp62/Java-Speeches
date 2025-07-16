package net.jmp.speeches.utils;

/*
 * (#)MetadataUtil.java 0.4.0   07/16/2025
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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.jmp.speeches.documents.MongoSpeechDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.jmp.util.logging.LoggerUtils.*;
import static net.jmp.util.logging.LoggerUtils.entryWith;

/// The metadata utility class.
///
/// @version    0.4.0
/// @since      0.4.0
public final class MetadataUtil {
    /// The logger.
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /// The MongoDB client.
    private MongoClient mongoClient;

    /// The database name.
    private String dbName;

    /// The speeches collection name.
    private String speechesCollectionName;

    /// The set of speech titles.
    private final Set<String> titles = new HashSet<>();

    /// The set of speech authors.
    private final Set<String> authors = new HashSet<>();

    /// The map of speech author last names to their full names.
    private final Map<String, String> authorNames = new HashMap<>();

    /// Regular expression pattern to get the last word in a string (last name).
    private final Pattern patternLastWord = Pattern.compile("(\\w+)$");

    /// The constructor.
    ///
    /// @param  mongoClient             com.mongodb.client.MongoClient
    /// @param  dbName                  java.lang.String
    /// @param  speechesCollectionName  java.lang.String
    public MetadataUtil(final MongoClient mongoClient,
                        final String dbName,
                        final String speechesCollectionName) {
        super();

        this.mongoClient = mongoClient;
        this.dbName = dbName;
        this.speechesCollectionName = speechesCollectionName;
    }

    /// Compile all possible author and title metadata values.
    ///
    /// @param  queryText  java.lang.String
    public void compileMetadataValues(final String queryText) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(queryText));
        }

        final Set<String> titlesInQuery = this.findTitles(queryText);
        final Set<String> authorsInQuery = this.findAuthorFullNames();

        authorsInQuery.addAll(this.findAuthorLastNames());

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

    /// Find the speech titles in the query text.
    ///
    /// @param  queryText   java.lang.String
    /// @return             java.util.Set<java.lang.String>
    private Set<String> findTitles(final String queryText) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(queryText));
        }

        final Set<String> results = new HashSet<>();

        this.titles.forEach(title -> {
            if (this.containsIgnoreCase(title, queryText)) {
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
    /// @param  queryText       java.lang.String
    /// @return                 boolean
    private boolean containsIgnoreCase(final String searchString, final String queryText) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(searchString, queryText));
        }

        final Pattern pattern = Pattern.compile(searchString, Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(queryText);
        final boolean result = matcher.find();

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(result));
        }

        return result;
    }
}
