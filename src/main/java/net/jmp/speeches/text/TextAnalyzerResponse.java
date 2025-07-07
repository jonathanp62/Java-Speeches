package net.jmp.speeches.text;

/*
 * (#)TextAnalyzerResponse.java 0.1.0   07/07/2025
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

/// The text analyzer response class.
///
/// @version    0.1.0
/// @since      0.1.0
public final class TextAnalyzerResponse {
    /// The title.
    private String title;

    /// The author.
    private String author;

    /// The text.
    private String text;

    /// The text size.
    private long size;

    /// The paragraphs.
    private final List<Paragraph> paragraphs = new ArrayList<>();

    /// The default constructor.
    public TextAnalyzerResponse() {
        super();
    }

    /// Set the title.
    ///
    /// @param  title   java.lang.String
    public void setTitle(final String title) {
        this.title = title;
    }

    /// Get the title.
    ///
    /// @return java.lang.String
    public String getTitle() {
        return this.title;
    }

    /// Set the author.
    ///
    /// @param  author  java.lang.String
    public void setAuthor(final String author) {
        this.author = author;
    }

    /// Get the author.
    ///
    /// @return java.lang.String
    public String getAuthor() {
        return this.author;
    }

    /// Set the text and capture the size.
    ///
    /// @param  text    java.lang.String
    public void setText(final String text) {
        this.text = text;
        this.size = text.length();
    }

    /// Get the text.
    ///
    /// @return java.lang.String
    public String getText() {
        return this.text;
    }

    /// Get the text size.
    ///
    /// @return long
    public long getSize() {
        return this.size;
    }

    /// Add a new paragraph to the collection.
    ///
    /// @param  paragraph   net.jmp.pinecone.quickstart.corenlp.TextAnalyzerResponse.Paragraph
    public void addParagraphs(final Paragraph paragraph) {
        this.paragraphs.add(paragraph);
    }

    /// Get the paragraphs.
    ///
    /// @return java.util.List<net.jmp.pinecone.quickstart.corenlp.TextAnalyzerResponse.Paragraph>
    public List<Paragraph> getParagraphs() {
        return this.paragraphs;
    }

    /// Get the number of paragraphs.
    ///
    /// @return int
    public int getNumberOfParagraphs() {
        return this.paragraphs.size();
    }

    /// Get the number of sentences.
    ///
    /// @return int
    public int getNumberOfSentences() {
        int count = 0;

        for (final Paragraph paragraph : this.paragraphs) {
            count += paragraph.getNumberOfSentences();
        }

        return count;
    }

    /// Get the number of tokens.
    ///
    /// @return int
    public int getNumberOfTokens() {
        int count = 0;

        for (final Paragraph paragraph : this.paragraphs) {
            count += paragraph.getNumberOfTokens();
        }

        return count;
    }

    /// The to-string method.
    ///
    /// @return java.lang.String
    @Override
    public String toString() {
        return "TextAnalyzerResponse{" +
                "title='" + this.title + '\'' +
                ", author='" + this.author + '\'' +
                ", text='" + this.text + '\'' +
                ", size=" + this.size +
                ", paragraphs=" + this.paragraphs +
                '}';
    }

    /// The paragraph class.
    public static class Paragraph {
        /// The paragraph number.
        private int number;

        /// The paragraph text.
        private String text;

        /// The paragraph size.
        private long size;

        /// The sentences.
        private final List<Sentence> sentences = new ArrayList<>();

        /// The default constructor.
        public Paragraph() {
            super();
        }

        /// Set the paragraph number.
        ///
        /// @param  number  int
        public void setNumber(final int number) {
            this.number = number;
        }

        /// Get the paragraph number.
        ///
        /// @return int
        public int getNumber() {
            return this.number;
        }

        /// Set the paragraph text and capture the size.
        ///
        /// @param  text    java.lang.String
        public void setText(final String text) {
            this.text = text;
            this.size = text.length();
        }

        /// Get the paragraph text.
        ///
        /// @return java.lang.String
        public String getText() {
            return this.text;
        }

        /// Get the paragraph size.
        ///
        /// @return long
        public long getSize() {
            return this.size;
        }

        /// Add a new sentence to the collection.
        ///
        /// @param  sentence    net.jmp.pinecone.quickstart.corenlp.TextAnalyzerResponse.Paragraph.Sentence
        public void addSentences(final Sentence sentence) {
            this.sentences.add(sentence);
        }

        /// Get the sentences.
        ///
        /// @return java.util.List<net.jmp.pinecone.quickstart.corenlp.TextAnalyzerResponse.Paragraph.Sentence>
        public List<Sentence> getSentences() {
            return this.sentences;
        }

        /// Get the number of sentences.
        ///
        /// @return int
        public int getNumberOfSentences() {
            return this.sentences.size();
        }

        /// Get the number of tokens.
        ///
        /// @return int
        public int getNumberOfTokens() {
            int count = 0;

            for (final Sentence sentence : this.sentences) {
                count += sentence.getNumberOfTokens();
            }

            return count;
        }

        /// The to-string method.
        ///
        /// @return java.lang.String
        @Override
        public String toString() {
            return "Paragraph{" +
                    "number=" + this.number +
                    ", text='" + this.text + '\'' +
                    ", size=" + this.size +
                    ", sentences=" + this.sentences +
                    '}';
        }

        /// The sentence class.
        public static class Sentence {
            /// The sentence number.
            private int number;

            /// The sentence text.
            private String text;

            /// The sentence size.
            private long size;

            /// The tokens.
            private List<String> tokens;

            /// The default constructor.
            public Sentence() {
                super();
            }

            /// Set the sentence number.
            ///
            /// @param  number  int
            public void setNumber(final int number) {
                this.number = number;
            }

            /// Get the sentence number.
            ///
            /// @return int
            public int getNumber() {
                return this.number;
            }

            /// Set the sentence text and capture the size.
            ///
            /// @param  text    java.lang.String
            public void setText(final String text) {
                this.text = text;
                this.size = text.length();
            }

            /// Get the sentence text.
            ///
            /// @return java.lang.String
            public String getText() {
                return this.text;
            }

            /// Get the sentence size.
            ///
            /// @return long
            public long getSize() {
                return this.size;
            }

            /// Set the tokens.
            ///
            /// @param  tokens  java.util.List<java.lang.String>
            public void setTokens(final List<String> tokens) {
                this.tokens = tokens;
            }

            /// Get the tokens.
            ///
            /// @return java.util.List<java.lang.String>
            public List<String> getTokens() {
                return this.tokens;
            }

            /// Get the number of tokens.
            ///
            /// @return int
            public int getNumberOfTokens() {
                return this.tokens.size();
            }

            /// The to-string method.
            ///
            /// @return java.lang.String
            @Override
            public String toString() {
                return "Sentence{" +
                        "number=" + this.number +
                        ", text='" + this.text + '\'' +
                        ", size=" + this.size +
                        ", tokens=" + this.tokens +
                        '}';
            }
        }
    }
}
