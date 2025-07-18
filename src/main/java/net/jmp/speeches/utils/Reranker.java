package net.jmp.speeches.utils;

/*
 * (#)Reranker.java 0.5.0   07/18/2025
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

import io.pinecone.clients.Inference;
import io.pinecone.clients.Pinecone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.jmp.util.logging.LoggerUtils.*;

import org.openapitools.inference.client.ApiException;

import org.openapitools.inference.client.model.RankedDocument;
import org.openapitools.inference.client.model.RerankResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The reranker utility class.
///
/// @version    0.5.0
/// @since      0.5.0
public final class Reranker {
    /// The logger.
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /// The Pinecone client.
    private final Pinecone pinecone;

    /// The reranking model.
    private final String rerankingModel;

    /// The query text.
    private final String queryText;

    /// The top N documents to rerank.
    private final int topN;

    /// The constructor.
    ///
    /// @param  pinecone        io.pinecone.clients.Pinecone
    /// @param  rerankingModel  java.lang.String
    /// @param  queryText       java.lang.String
    /// @param  topN            int
    public Reranker(final Pinecone pinecone,
                    final String rerankingModel,
                    final String queryText,
                    final int topN) {
        super();

        this.pinecone = pinecone;
        this.rerankingModel = rerankingModel;
        this.queryText = queryText;
        this.topN = topN;
    }

    /// Rerank the documents.
    ///
    /// @param  documents  java.util.List<java.util.Map<java.lang.String,java.lang.Object>>
    /// @return            java.util.List<java.lang.String>
    public List<String> rerank(final List<Map<String, Object>> documents) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(documents));
        }

        final List<String> rankFields = List.of("text_segment");

        /* Create the parameters for the reranking model */

        final Map<String, Object> parameters = new HashMap<>();

        parameters.put("truncate", "END");

        /* Perform the reranking */

        final Inference inference = this.pinecone.getInferenceClient();

        RerankResult result = null;

        try {
            result = inference.rerank(
                    this.rerankingModel,
                    this.queryText,
                    documents,
                    rankFields,
                    this.topN,
                    true,
                    parameters
            );
        } catch (ApiException e) {
            this.logger.error(e.getMessage());
        }

        /* Create the list of ranked text segments */

        final List<String> rankedTextSegments = new ArrayList<>();

        if (result != null) {
            final List<RankedDocument> rankedDocuments = result.getData();

            for (final RankedDocument rankedDocument : rankedDocuments) {
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Document: {}", rankedDocument.toJson());
                }

                final Map<String, Object> document = rankedDocument.getDocument();

                assert document != null;

                final String textSegment = (String) document.get("text_segment");

                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Text segment : {}", textSegment);
                }

                rankedTextSegments.add(textSegment);
            }
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(rankedTextSegments));
        }

        return rankedTextSegments;
    }
}
