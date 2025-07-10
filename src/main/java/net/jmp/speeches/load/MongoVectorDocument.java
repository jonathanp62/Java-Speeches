package net.jmp.speeches.load;

/*
 * (#)MongoVectorDocument.java  0.3.0   07/10/2025
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

import org.bson.BsonType;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonRepresentation;

/// The MongoDB vector document class.
///
/// @version    0.3.0
/// @since      0.3.0
public class MongoVectorDocument {
    /// The MongoDB document identifier.
    @BsonId
    @BsonRepresentation(BsonType.OBJECT_ID)
    private String id;

    /// The speech identifier.
    private String speechId;

    /// The title.
    private String title;

    /// The author.
    private String author;

    /// The vector identifiers.
    private List<String> vectorIds = new ArrayList<>();

    /// The default constructor.
    @BsonCreator
    public MongoVectorDocument() {
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

    /// Get the speech identifier.
    ///
    /// @return java.lang.String
    public String getSpeechId() {
        return this.speechId;
    }

    /// Set the speech identifier.
    ///
    /// @param  speechId    java.lang.String
    public void setSpeechId(final String speechId) {
        this.speechId = speechId;
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

    /// Get the vector identifiers.
    ///
    /// @return java.util.List<java.lang.String>
    public List<String> getVectorIds() {
        return this.vectorIds;
    }

    /// Add a vector identifier to the collection.
    ///
    /// @param  vectorId    java.lang.String
    public void addVectorId(final String vectorId) {
        this.vectorIds.add(vectorId);
    }
}
