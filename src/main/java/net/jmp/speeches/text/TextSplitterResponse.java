package net.jmp.speeches.text;

/*
 * (#)TextSplitterResponse.java 0.3.0   07/08/2025
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

import java.util.ArrayList;
import java.util.List;

/// The text splitter response class.
///
/// @version    0.3.0
/// @since      0.3.0
public final class TextSplitterResponse {
    /// The maximum number of tokens.
    private int maxTokens;

    /// The total number of tokens.
    private int totalTokens;

    /// The number of paragraphs.
    private int numberOfParagraphs;

    /// The list of paragraphs.
    private final List<Paragraph> paragraphs = new ArrayList<>();

    /// The list of text segments.
    private final List<String> textSegments = new ArrayList<>();

    /// The default constructor.
    public TextSplitterResponse() {
        super();
    }

    /// Get the maximum number of tokens.
    ///
    /// @return int
    public int getMaxTokens() {
        return this.maxTokens;
    }

    /// Set the maximum number of tokens.
    ///
    /// @param  maxTokens   int
    public void setMaxTokens(final int maxTokens) {
        this.maxTokens = maxTokens;
    }

    /// Get the total number of tokens.
    ///
    /// @return int
    public int getTotalTokens() {
        return this.totalTokens;
    }

    /// Set the total number of tokens.
    ///
    /// @param  totalTokens int
    public void setTotalTokens(final int totalTokens) {
        this.totalTokens = totalTokens;
    }

    /// Get the number of paragraphs.
    ///
    /// @return int
    public int getNumberOfParagraphs() {
        return this.numberOfParagraphs;
    }

    /// Set the number of paragraphs.
    ///
    /// @param  numberOfParagraphs  int
    public void setNumberOfParagraphs(final int numberOfParagraphs) {
        this.numberOfParagraphs = numberOfParagraphs;
    }

    /// Get the list of paragraphs.
    ///
    /// @return java.util.List<net.jmp.speeches.text.TextSplitterResponse.Paragraph>
    public List<Paragraph> getParagraphs() {
        return this.paragraphs;
    }

    /// Get the number of text segments.
    ///
    /// @return int
    public int getNumberOfTextSegments() {
        return this.textSegments.size();
    }

    /// Get the list of text segments.
    ///
    /// @return java.util.List<java.lang.String>
    public List<String> getTextSegments() {
        return this.textSegments;
    }

    /// The paragraph class.
    public static class Paragraph {
        /// The paragraph number.
        private final int number;

        /// The paragraph text.
        private final String text;

        /// The number of tokens in the paragraph.
        private final int tokens;

        /// The number of text segments in the paragraph.
        private final int textSegments;

        /// The constructor.
        ///
        /// @param  number          int
        /// @param  text            java.lang.String
        /// @param  tokens          int
        /// @param  textSegments    int
        public Paragraph(final int number,
                         final String text,
                         final int tokens,
                         final int textSegments) {
            super();

            this.number = number;
            this.text = text;
            this.tokens = tokens;
            this.textSegments = textSegments;
        }

        /// Get the paragraph number.
        ///
        /// @return int
        public int getNumber() {
            return this.number;
        }

        /// Get the paragraph text.
        ///
        /// @return java.lang.String
        public String getText() {
            return this.text;
        }

        /// Get the paragraph tokens.
        ///
        /// @return int
        public int getTokens() {
            return this.tokens;
        }

        /// Get the paragraph text segments.
        ///
        /// @return int
        public int getTextSegments() {
            return this.textSegments;
        }

        /// The to-string method.
        ///
        /// @return java.lang.String
        @Override
        public String toString() {
            return "Paragraph{" +
                    "number=" + this.number +
                    ", text='" + this.text + '\'' +
                    ", tokens=" + this.tokens +
                    ", textSegments=" + this.textSegments +
                    '}';
        }
    }

    /// The to-string method.
    ///
    /// @return java.lang.String
    @Override
    public String toString() {
        return "TextSplitterResponse{" +
                "maxTokens=" + this.maxTokens +
                ", totalTokens=" + this.totalTokens +
                ", numberOfParagraphs=" + this.numberOfParagraphs +
                ", paragraphs=" + this.paragraphs +
                ", textSegments=" + this.textSegments +
                '}';
    }
}
