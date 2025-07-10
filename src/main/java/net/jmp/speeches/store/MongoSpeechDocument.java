package net.jmp.speeches.store;

/*
 * (#)MongoSpeechDocument.java  0.3.0   07/09/2025
 * (#)MongoSpeechDocument.java  0.1.0   07/07/2025
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

import net.jmp.speeches.text.TextAnalyzerResponse;

import org.bson.BsonType;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonRepresentation;

/// The MongoDB document class.
///
/// @version    0.3.0
/// @since      0.1.0
public class MongoSpeechDocument {
    /// The MongoDB document identifier.
    @BsonId
    @BsonRepresentation(BsonType.OBJECT_ID)
    private String id;

    /// The file name.
    private String fileName;

    /// The file size.
    private long fileSize;

    /// The total number of paragraphs.
    private long totalParagraphs;

    /// The total number of sentences.
    private long totalSentences;

    /// The total number of tokens.
    private long totalTokens;

    /// The text analysis response.
    private TextAnalyzerResponse textAnalysis;

    /// The default constructor.
    @BsonCreator
    public MongoSpeechDocument() {
        super();
    }

    /// Get the document identifier.
    ///
    /// @return java.lang.String
    public String getId() {
        return this.id;
    }

    /// Set the document identifier.
    ///
    /// @param  id  java.lang.String
    public void setId(final String id) {
        this.id = id;
    }

    /// Get the file name.
    ///
    /// @return java.lang.String
    public String getFileName() {
        return this.fileName;
    }

    /// Set the file name.
    ///
    /// @param  fileName    java.lang.String
    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    /// Get the file size.
    ///
    /// @return long
    public long getFileSize() {
        return this.fileSize;
    }

    /// Set the file size.
    ///
    /// @param  fileSize    long
    public void setFileSize(final long fileSize) {
        this.fileSize = fileSize;
    }

    /// Get the total paragraphs.
    ///
    /// @return long
    public long getTotalParagraphs() {
        return this.totalParagraphs;
    }

    /// Set the total paragraphs.
    ///
    /// @param  totalParagraphs long
    public void setTotalParagraphs(final long totalParagraphs) {
        this.totalParagraphs = totalParagraphs;
    }

    /// Get the total sentences.
    ///
    /// @return long
    public long getTotalSentences() {
        return this.totalSentences;
    }

    /// Set the total sentences.
    ///
    /// @param  totalSentences  long
    public void setTotalSentences(final long totalSentences) {
        this.totalSentences = totalSentences;
    }

    /// Get the total tokens.
    ///
    /// @return long
    public long getTotalTokens() {
        return this.totalTokens;
    }

    /// Set the total tokens.
    ///
    /// @param  totalTokens long
    public void setTotalTokens(final long totalTokens) {
        this.totalTokens = totalTokens;
    }

    /// Get the text analysis.
    ///
    /// @return net.jmp.speeches.text.TextAnalyzerResponse
    public TextAnalyzerResponse getTextAnalysis() {
        return this.textAnalysis;
    }

    /// Set the text analysis.
    ///
    /// @param  textAnalysis    net.jmp.speeches.text.TextAnalyzerResponse
    public void setTextAnalysis(final TextAnalyzerResponse textAnalysis) {
        this.textAnalysis = textAnalysis;
    }
}
