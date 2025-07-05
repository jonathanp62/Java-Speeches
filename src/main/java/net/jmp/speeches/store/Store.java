package net.jmp.speeches.store;

/*
 * (#)Store.java    0.1.0   07/05/2025
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

import java.io.File;
import java.io.IOException;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;

import java.nio.file.attribute.BasicFileAttributes;

import java.util.ArrayList;
import java.util.List;

import java.util.function.UnaryOperator;

import net.jmp.speeches.Operation;

import static net.jmp.util.logging.LoggerUtils.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The store speeches into MongoDB class.
///
/// @version    0.1.0
/// @since      0.1.0
public final class Store extends Operation {
    /// The logger.
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /// The constructor.
    ///
    /// @param  builder net.jmp.speeches.store.Store.Builder
    public Store(final Builder builder) {
        super(Operation.operationBuilder()
                .dbName(builder.dbName)
                .collectionName(builder.collectionName)
                .mongoClient(builder.mongoClient)
                .speechesLocation(builder.speechesLocation)
        );
    }

    /// Return the builder.
    ///
    /// @return  net.jmp.speeches.store.Store.Builder
    public static Builder builder() {
        return new Builder();
    }

    /// The operate method.
    @Override
    public void operate() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        this.logger.info("Storing speeches from: {}", this.speechesLocation);

        List<File> files = null;

        try {
            files = this.getSpeechFiles();
        } catch (final IOException ioe) {
            this.logger.error(catching(ioe));
        }

        if (files != null) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Found {} files", files.size());

                files.forEach(file -> this.logger.debug("Found file: {}", file.getAbsolutePath()));
            }

            try {
                for (final File file : files) {
                    this.handleSpeech(file);
                }
            } catch (final IOException ioe) {
                this.logger.error(catching(ioe));
            }
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Handle the speech.
    ///
    /// @param  file    java.io.File
    /// @throws         java.io.IOException When an I/O error occurs
    private void handleSpeech(final File file) throws IOException {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(file));
        }

        final UnaryOperator<String> fileNameToTitle = fileName -> fileName.substring(0, fileName.indexOf('.')).replace('-', ' ');

        final String author = this.getAuthor(file.getCanonicalFile().getParentFile().getName());
        final String title = fileNameToTitle.apply(file.getName());
        final String fileName = file.getName();
        final long fileSize = file.length();

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Author   : {}", author);
            this.logger.debug("Title    : {}", title);
            this.logger.debug("File name: {}", fileName);
            this.logger.debug("File size: {}", fileSize);
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Get the author.
    ///
    /// @param  parentName  java.lang.String
    /// @return             java.lang.String
    private String getAuthor(final String parentName) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(parentName));
        }

        String author;

        switch (parentName) {
            case "Anthony"-> author = "Susan B. Anthony";
            case "Bush"-> author = "George Bush";
            case "Carter"-> author = "Jimmy Carter";
            case "Clinton"-> author = "Bill Clinton";
            case "Douglass"-> author = "Frederick Douglass";
            case "Eisenhower"-> author = "Dwight D. Eisenhower";
            case "Ford"-> author = "Gerald R. Ford";
            case "Gore"-> author = "Al Gore";
            case "Henry"-> author = "Patrick Henry";
            case "Johnson"-> author = "Lyndon B. Johnson";
            case "Kennedy"-> author = "John F. Kennedy";
            case "Lincoln"-> author = "Abraham Lincoln";
            case "Nixon"-> author = "Richard M. Nixon";
            case "Reagan"-> author = "Ronald Reagan";
            case "Roosevelt"-> author = "Franklin D. Roosevelt";
            case "Washington"-> author = "George Washington";
            case "Wilson"-> author = "Woodrow Wilson";
            default -> author = "Unknown";
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(author));
        }

        return author;
    }

    /// Get the speech files.
    ///
    /// @return java.util.List<java.io.File>
    /// @throws java.io.IOException When an I/O error occurs
    private List<File> getSpeechFiles() throws IOException {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        final List<File> filesList = new ArrayList<>();

        Files.walkFileTree(Path.of(this.speechesLocation), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
                final File file = path.toFile();

                filesList.add(path.toFile());

                if (logger.isDebugEnabled()) {
                    logger.debug("Adding file: {}", file.getAbsolutePath());
                }

                return FileVisitResult.CONTINUE;
            }
        });

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(filesList));
        }

        return filesList;
    }

    /// The builder class.
    public static class Builder {
        /// The mongo client.
        private MongoClient mongoClient;

        /// The collection name.
        private String collectionName;

        /// The database name.
        private String dbName;

        /// The location of the speeches.
        private String speechesLocation;

        /// The default constructor.
        public Builder() {
            super();
        }

        /// Set the mongo client.
        ///
        /// @param  mongoClient io.mongodb.client.MongoClient
        /// @return             net.jmp.pinecone.speeches.store.Store.Builder
        public Builder mongoClient(final MongoClient mongoClient) {
            this.mongoClient = mongoClient;

            return this;
        }

        /// Set the collection name.
        ///
        /// @param  collectionName java.lang.String
        /// @return                net.jmp.pinecone.speeches.store.Store.Builder
        public Builder collectionName(final String collectionName) {
            this.collectionName = collectionName;

            return this;
        }

        /// Set the database name.
        ///
        /// @param  dbName java.lang.String
        /// @return        net.jmp.pinecone.speeches.store.Store.Builder
        public Builder dbName(final String dbName) {
            this.dbName = dbName;

            return this;
        }

        /// Set the location of the speeches.
        ///
        /// @param  speechesLocation    java.lang.String
        /// @return                     net.jmp.pinecone.speeches.store.Store.Builder
        public Builder speechesLocation(final String speechesLocation) {
            this.speechesLocation = speechesLocation;

            return this;
        }

        /// Build the object.
        ///
        /// @return net.jmp.speeches.Store
        public Store build() {
            return new Store(this);
        }
    }
}
