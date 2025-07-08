package net.jmp.speeches.text;

/*
 * (#)TextSplitter.java 0.3.0   07/08/2025
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

/// The text splitter class.
///
/// @version    0.3.0
/// @since      0.3.0
public final class TextSplitter {
    /// The logger.
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /// The document.
    private final String document;

    /// The max tokens.
    private final int maxTokens;

    /// The average length of an English word.
    private static final int AVERAGE_ENGLISH_WORD_LENGTH = 5;

    /// The sentence tokens logging message.
    private static final String SENTENCE_TOKENS = "Sentence tokens: {}";

    /// The core NLP pipeline.
    final StanfordCoreNLP pipeline;

    /// The response.
    final TextSplitterResponse response = new TextSplitterResponse();

    /// The constructor.
    ///
    /// @param  builder net.jmp.speeches.text.TextSplitter.Builder
    private TextSplitter(final Builder builder) {
        super();

        this.document = builder.document;
        this.maxTokens = builder.maxTokens;

        final Properties props = new Properties();    // Set up pipeline properties

        /* Set the list of annotators to run - The order is significant */

        props.setProperty("annotators", "tokenize");

        /* Set up the pipeline */

        this.pipeline = new StanfordCoreNLP(props);
    }

    /// Return an instance of the builder.
    ///
    /// @return net.jmp.speeches.text.TextSplitter.Builder
    public static Builder builder() {
        return new Builder();
    }

    /// Split the document into text segments
    /// and returns a text splitter response.
    ///
    /// @return net.jmp.speeches.text.TextSplitterResponse
    public TextSplitterResponse split() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        final List<String> textSegments = this.response.getTextSegments();

        /* Determine the total number of tokens in the document */

        final CoreDocument entireDocument = new CoreDocument(this.document);

        pipeline.annotate(entireDocument);

        final int entireDocumentTotalTokens = entireDocument.tokens().size();

        this.response.setMaxTokens(this.maxTokens);
        this.response.setTotalTokens(entireDocumentTotalTokens);

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Max tokens  : {}", this.maxTokens);
            this.logger.debug("Total tokens: {}", entireDocumentTotalTokens);
        }

        if (entireDocumentTotalTokens <= this.maxTokens) {
            textSegments.add(this.document);
        } else {
            final String[] paragraphs = this.document.split("\\R\\R");

            this.response.setNumberOfParagraphs(paragraphs.length);

            this.logger.debug("Paragraphs: {}", paragraphs.length);

            int countParapgraphs = 0;

            for (final String paragraph : paragraphs) {
                final CoreDocument coreDocument = new CoreDocument(paragraph);

                this.pipeline.annotate(coreDocument);

                final int paragraphTotalTokens = coreDocument.tokens().size();

                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("Paragraph tokens: {}", paragraphTotalTokens);    // This matches total tokens below
                }

                /* Check the paragraph as a whole */

                if (paragraphTotalTokens <= this.maxTokens) {
                    this.response.getParagraphs().add(new TextSplitterResponse.Paragraph(
                            ++countParapgraphs,
                            paragraph,
                            paragraphTotalTokens,
                            1
                    ));

                    textSegments.add(paragraph);

                    continue;
                }

                /* Process the paragraph by sentences */

                final List<String> addedTextSegments = this.handleLongParagraph(coreDocument);

                this.response.getParagraphs().add(new TextSplitterResponse.Paragraph(
                        ++countParapgraphs,
                        paragraph,
                        paragraphTotalTokens,
                        addedTextSegments.size()
                ));

                textSegments.addAll(addedTextSegments);
            }
        }

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Total text segments: {}", textSegments.size());
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(this.response));
        }

        return this.response;
    }

    /// Handle a long paragraph by breaking it into sentences.
    /// A list of text segments is returned.
    ///
    /// @param  coreDocument    edu.stanford.nlp.pipeline.CoreDocument
    /// @return                 java.util.List<java.lang.String>
    private List<String> handleLongParagraph(final CoreDocument coreDocument) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(coreDocument));
        }

        final List<String> textSegments = new ArrayList<>();

        final StringBuilder sentenceBuilder = new StringBuilder(this.maxTokens * AVERAGE_ENGLISH_WORD_LENGTH);

        int totalTokens = 0;

        for (final CoreSentence sentence : coreDocument.sentences()) {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Core sentence: {}", sentence.text());
            }

            final int tokensInSentence = sentence.tokensAsStrings().size();

            if (this.logger.isDebugEnabled()) {
                this.logger.debug(SENTENCE_TOKENS, tokensInSentence);
                this.logger.debug(SENTENCE_TOKENS, sentence.tokensAsStrings());
            }

            totalTokens = this.handleSentence(sentence, sentenceBuilder, textSegments, totalTokens);
        }

        if (!sentenceBuilder.isEmpty()) {
            textSegments.add(sentenceBuilder.toString());    // Add any remaining sentences to the text segments
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(textSegments));
        }

        return textSegments;
    }

    /// Handle a sentence. The updated
    /// total number of tokens is returned.
    ///
    /// @param  sentence        edu.stanford.nlp.trees.CoreSentence
    /// @param  sentenceBuilder java.lang.StringBuilder
    /// @param  textSegments    java.util.List<java.lang.String>
    /// @param  totalTokens     int
    /// @return                 int
    private int handleSentence(final CoreSentence sentence,
                               final StringBuilder sentenceBuilder,
                               final List<String> textSegments,
                               final int totalTokens) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(sentence, sentenceBuilder, textSegments, totalTokens));
        }

        int countTokens = totalTokens;

        final int tokensInSentence = sentence.tokensAsStrings().size();

        if (this.logger.isDebugEnabled()) {
            this.logger.debug(SENTENCE_TOKENS, tokensInSentence);
            this.logger.debug(SENTENCE_TOKENS, sentence.tokensAsStrings());
        }

        if (tokensInSentence > this.maxTokens) {
            /* Flush any sentences in the sentence builder to the result strings */

            if (!sentenceBuilder.isEmpty()) {
                textSegments.add(sentenceBuilder.toString());   // Add to the text segments
                sentenceBuilder.setLength(0);                   // Reset the sentence builder
            }

            /* Process the sentence by words */

            textSegments.addAll(this.handleLongSentence(sentence));
        } else {
            if (totalTokens + tokensInSentence <= this.maxTokens) {     // Sentence fits
                sentenceBuilder.append(sentence.text()).append(" ");    // Add the sentence

                countTokens += tokensInSentence;
            } else {
                if (!sentenceBuilder.isEmpty()) {                       // Sentence will exceed the token limit
                    textSegments.add(sentenceBuilder.toString());       // Add to the text segments
                    sentenceBuilder.setLength(0);                       // Reset the sentence builder
                }

                sentenceBuilder.append(sentence.text()).append(" ");    // Add the sentence

                countTokens = tokensInSentence;
            }
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(countTokens));
        }

        return countTokens;
    }

    /// Handle a long sentence by breaking it into words.
    /// A list of text segments is returned.
    ///
    /// @param  sentence    edu.stanford.nlp.trees.CoreSentence
    /// @return             java.util.List<java.lang.String>
    private List<String> handleLongSentence(final CoreSentence sentence) {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entryWith(sentence));
        }

        final List<String> textSegments = new ArrayList<>();

        /* Process the sentence by words */

        final StringBuilder wordBuilder = new StringBuilder(this.maxTokens * AVERAGE_ENGLISH_WORD_LENGTH);

        int wordTokens = 0;

        for (final String word : sentence.tokensAsStrings()) {
            if (wordTokens + 1 <= this.maxTokens) {
                wordBuilder.append(word).append(" ");

                ++wordTokens;
            } else {
                textSegments.add(wordBuilder.toString());   // Add to the text segments
                wordBuilder.setLength(0);                   // Reset the word builder
                wordBuilder.append(word).append(" ");       // Add the word

                wordTokens = 1;
            }
        }

        textSegments.add(wordBuilder.toString());   // Add any remaining words to the text segments

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exitWith(textSegments));
        }

        return textSegments;
    }

    /// The builder class.
    public static class Builder {
        /// The document.
        private String document;

        /// The max tokens.
        private int maxTokens;

        /// The default constructor.
        private Builder () {
            super();
        }

        /// Set the document.
        ///
        /// @param  document    java.lang.String
        /// @return             net.jmp.speeches.text.TextSplitter.Builder
        public Builder document(final String document) {
            this.document = document;

            return this;
        }

        /// Set the max tokens.
        ///
        /// @param  maxTokens   int
        /// @return             net.jmp.speeches.text.TextSplitter.Builder
        public Builder maxTokens(final int maxTokens) {
            this.maxTokens = maxTokens;

            return this;
        }

        /// Build the text splitter object.
        ///
        /// @return net.jmp.speeches.text.TextSplitter
        public TextSplitter build() {
            return new TextSplitter(this);
        }
    }
}
