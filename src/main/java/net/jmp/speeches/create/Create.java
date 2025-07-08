package net.jmp.speeches.create;

/*
 * (#)Create.java   0.2.0   07/08/2025
 * (#)Create.java   0.1.0   07/05/2025
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

import java.util.Map;

import net.jmp.speeches.Operation;

import static net.jmp.util.logging.LoggerUtils.*;

import org.openapitools.db_control.client.ApiException;

import org.openapitools.db_control.client.model.CreateIndexForModelRequest;
import org.openapitools.db_control.client.model.CreateIndexForModelRequestEmbed;
import org.openapitools.db_control.client.model.DeletionProtection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The create Pinecone index class.
///
/// @version    0.2.0
/// @since      0.1.0
public final class Create extends Operation {
    /// The logger.
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    /// The constructor.
    ///
    /// @param  builder net.jmp.speeches.create.Create.Builder
    private Create(final Builder builder) {
        super(Operation.operationBuilder()
                .searchableIndexName(builder.searchableIndexName)
                .searchableEmbeddingModel(builder.searchableEmbeddingModel)
                .namespace(builder.namespace)
                .pinecone(builder.pinecone)
        );
    }

    /// Return the builder.
    ///
    /// @return  net.jmp.speeches.create.Create.Builder
    public static Builder builder() {
        return new Builder();
    }

    /// The operate method.
    @Override
    public void operate() {
        if (this.logger.isTraceEnabled()) {
            this.logger.trace(entry());
        }

        this.logger.info("Searchable index: {}", this.searchableIndexName);
        this.logger.info("Embedding model : {}", this.searchableEmbeddingModel);
        this.logger.info("Namespace       : {}", this.namespace);

        if (!this.doesSearchableIndexExist()) {
            this.logger.info("Creating searchable index: {}", this.searchableIndexName);

            final Map<String, String> fieldMap = Map.of("text", "text_segment");   // The name of the text field from your document model that will be embedded
            final Map<String, String> tags = Map.of("env", "development");

            final CreateIndexForModelRequestEmbed embed = new CreateIndexForModelRequestEmbed();

            embed.model(this.searchableEmbeddingModel)
                    .metric(CreateIndexForModelRequestEmbed.MetricEnum.COSINE)
                    .dimension(1024)
                    .fieldMap(fieldMap);

            try {
                this.pinecone.createIndexForModel(
                        this.searchableIndexName,
                        CreateIndexForModelRequest.CloudEnum.AWS,
                        "us-east-1",
                        embed,
                        DeletionProtection.DISABLED,
                        tags
                );
            } catch (final ApiException ae) {
                this.logger.error(catching(ae));
            }
        } else {
            this.logger.info("Searchable index already exists: {}", this.searchableIndexName);
        }

        if (this.logger.isTraceEnabled()) {
            this.logger.trace(exit());
        }
    }

    /// The builder class.
    public static class Builder {
        /// The Pinecone client.
        private Pinecone pinecone;

        /// The searchable embedding model.
        private String searchableEmbeddingModel;

        /// The searchable index name.
        private String searchableIndexName;

        /// The namespace.
        private String namespace;

        /// The default constructor.
        private Builder() {
            super();
        }

        /// Set the Pinecone client.
        ///
        /// @param  pinecone    io.pinecone.clients.Pinecone
        /// @return             net.jmp.speeches.create.Create.Builder
        public Builder pinecone(final Pinecone pinecone) {
            this.pinecone = pinecone;

            return this;
        }

        /// Set the searchable embedding model.
        ///
        /// @param  searchableEmbeddingModel    java.lang.String
        /// @return                             net.jmp.speeches.create.Create.Builder
        public Builder searchableEmbeddingModel(final String searchableEmbeddingModel) {
            this.searchableEmbeddingModel = searchableEmbeddingModel;

            return this;
        }

        /// Set the searchable index name.
        ///
        /// @param  searchableIndexName java.lang.String
        /// @return                     net.jmp.speeches.create.Create.Builder
        public Builder searchableIndexName(final String searchableIndexName) {
            this.searchableIndexName = searchableIndexName;

            return this;
        }

        /// Set the namespace.
        ///
        /// @param  namespace   java.lang.String
        /// @return             net.jmp.speeches.create.Create.Builder
        public Builder namespace(final String namespace) {
            this.namespace = namespace;

            return this;
        }

        /// Build the create object.
        ///
        /// @return net.jmp.speeches.create.Create
        public Create build() {
            return new Create(this);
        }
    }
}
