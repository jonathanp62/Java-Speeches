package net.jmp.speeches.utils;

/*
 * (#)MongoUtils.java   0.4.0   07/14/2025
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

import java.util.ArrayList;
import java.util.List;

import net.jmp.speeches.documents.MongoSpeechDocument;

import net.jmp.speeches.text.TextAnalyzerResponse;

import static net.jmp.util.logging.LoggerUtils.*;

import org.bson.conversions.Bson;

import org.slf4j.Logger;

/// The MongoDB utility class.
///
/// @version    0.3.0
/// @since      0.1.0
public final class MongoUtils {
    /// The default constructor.
    private MongoUtils() {
        super();
    }

    /// Get the Mongo speech documents.
    ///
    /// @param   logger                 org.slf4j.Logger
    /// @param   mongoClient            com.mongodb.client.MongoClient
    /// @param   dbName                 java.lang.String
    /// @param   speechesCollectionName java.lang.String
    /// @return                         java.util.List<net.jmp.speeches.documents.MongoSpeechDocument>
    public static List<MongoSpeechDocument> getSpeechDocuments(final Logger logger,
                                                         final MongoClient mongoClient,
                                                         final String dbName,
                                                         final String speechesCollectionName) {
        if (logger.isTraceEnabled()) {
            logger.trace(entryWith(mongoClient, dbName, speechesCollectionName));
        }

        List<MongoSpeechDocument> speechDocuments;

        final MongoDatabase database = mongoClient.getDatabase(dbName);
        final MongoCollection<MongoSpeechDocument> collection = database.getCollection(speechesCollectionName, MongoSpeechDocument.class);

        final Bson projectionFields = Projections.fields(
                Projections.include("textAnalysis")
        );

        try (final MongoCursor<MongoSpeechDocument> cursor = collection
                .find()
                .projection(projectionFields)
                .iterator()) {
            if (logger.isDebugEnabled()) {
                logger.debug("There are {} documents available", cursor.available());
            }

            speechDocuments = new ArrayList<>(cursor.available());

            while (cursor.hasNext()) {
                final MongoSpeechDocument speechDocument = cursor.next();
                final TextAnalyzerResponse textAnalysis = speechDocument.getTextAnalysis();

                if (logger.isDebugEnabled()) {
                    logger.debug("ID    : {}", speechDocument.getId());
                    logger.debug("Title : {}", textAnalysis.getTitle());
                    logger.debug("Author: {}", textAnalysis.getAuthor());
                }

                speechDocuments.add(speechDocument);
            }
        }

        if (logger.isTraceEnabled()) {
            logger.trace(exitWith(speechDocuments));
        }

        return speechDocuments;
    }
}
