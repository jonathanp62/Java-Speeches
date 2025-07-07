package net.jmp.speeches.text;

/*
 * (#)TextAnalyzer.java 0.1.0   07/07/2025
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

import edu.stanford.nlp.pipeline.*;

import java.util.*;

import static net.jmp.util.logging.LoggerUtils.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The text analyzer class.
///
/// @version    0.1.0
/// @since      0.1.0
public final class TextAnalyzer {
    /// The logger.
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /// The document text.
    private final String text;

    /// The document title.
    private final String title;

    /// The document author.
    private final String author;

    /// The core NLP pipeline.
    final StanfordCoreNLP pipeline;

    /// The response.
    final TextAnalyzerResponse response = new TextAnalyzerResponse();

    /// The constructor.
    ///
    /// @param  builder net.jmp.pinecone.quickstart.corenlp.TextAnalyzer.Builder
    public TextAnalyzer(final Builder builder) {
        super();

        this.text = builder.text;
        this.title = builder.title;
        this.author = builder.author;

        final Properties props = new Properties();    // Set up pipeline properties

        /* Set the list of annotators to run - The order is significant */

        props.setProperty("annotators", "tokenize");

        /* Set up the pipeline */

        this.pipeline = new StanfordCoreNLP(props);
    }

    /// Get the builder instance.
    ///
    /// @return     net.jmp.pinecone.quickstart.corenlp.TextAnalyzer.Builder
    public static Builder builder() { return new Builder(); }

    /// Analyze the document and return the response.
    ///
    /// @return     net.jmp.pinecone.quickstart.corenlp.TextAnalyzerResponse
    public TextAnalyzerResponse analyze() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        this.response.setTitle(this.title);
        this.response.setAuthor(this.author);
        this.response.setText(this.text);

        this.handleParagraphs();

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(this.response));
        }

        return this.response;
    }

    /// Handle the paragraphs.
    private void handleParagraphs() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        final String[] paragraphs = this.text.split("\\R\\R");

        int paragraphNumber = 0;

        for (final String string : paragraphs) {
            final TextAnalyzerResponse.Paragraph paragraph = new TextAnalyzerResponse.Paragraph();

            paragraph.setNumber(paragraphNumber++);
            paragraph.setText(string);

            this.handleSentences(paragraph);

            this.response.addParagraphs(paragraph);
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// Handle the sentences in the paragraph.
    ///
    /// @param  paragraph   net.jmp.pinecone.quickstart.corenlp.TextAnalyzerResponse.Paragraph
    private void handleSentences(final TextAnalyzerResponse.Paragraph paragraph) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(paragraph));
        }

        final CoreDocument document = new CoreDocument(paragraph.getText());

        this.pipeline.annotate(document);

        int sentenceNumber = 0;

        for (final CoreSentence coreSentence : document.sentences()) {
            final TextAnalyzerResponse.Paragraph.Sentence sentence = new TextAnalyzerResponse.Paragraph.Sentence();

            sentence.setNumber(sentenceNumber++);
            sentence.setText(coreSentence.text());
            sentence.setTokens(coreSentence.tokensAsStrings());

            paragraph.addSentences(sentence);
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// The builder class.
    public static class Builder {
        /// The document text.
        private String text;

        /// The document title.
        private String title;

        /// The document author.
        private String author;

        /// The default constructor.
        public Builder () {
            super();
        }

        /// Set the document text.
        ///
        /// @param  text    java.lang.String
        /// @return         net.jmp.pinecone.quickstart.corenlp.TextAnalyzer.Builder
        public Builder text(final String text) {
            this.text = text;

            return this;
        }

        /// Set the document title.
        ///
        /// @param  title   java.lang.String
        /// @return         net.jmp.pinecone.quickstart.corenlp.TextAnalyzer.Builder
        public Builder title(final String title) {
            this.title = title;

            return this;
        }

        /// Set the document author.
        ///
        /// @param  author  java.lang.String
        /// @return         net.jmp.pinecone.quickstart.corenlp.TextAnalyzer.Builder
        public Builder author(final String author) {
            this.author = author;

            return this;
        }

        /// Build the text analyzer.
        ///
        /// @return net.jmp.pinecone.quickstart.corenlp.TextAnalyzer
        public TextAnalyzer build() {
            return new TextAnalyzer(this);
        }
    }
}
