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

import io.pinecone.clients.Pinecone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.jmp.util.logging.LoggerUtils.*;

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

        final List<String> results = new ArrayList<>();

        for (final Map<String, Object> document : documents) {
            results.add((String) document.get("text_segment"));
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(results));
        }

        return results;
    }
}
