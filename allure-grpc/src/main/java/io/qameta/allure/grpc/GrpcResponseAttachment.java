/*
 *  Copyright 2016-2026 Qameta Software Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.grpc;

import io.qameta.allure.attachment.AttachmentData;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class GrpcResponseAttachment implements AttachmentData {

    private final String name;
    private final String body;
    private final String status;
    private final Map<String, String> metadata;

    public GrpcResponseAttachment(final String name,
                                  final String body,
                                  final String status,
                                  final Map<String, String> metadata) {
        this.name = name;
        this.body = body;
        this.status = status;
        this.metadata = metadata;
    }

    public String getBody() {
        return body;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Builder for GrpcResponseAttachment.
     */
    static final class Builder {

        private final String name;
        private final Map<String, String> metadata = new HashMap<>();
        private String body;
        private String status;

        private Builder(final String name) {
            Objects.requireNonNull(name, "Name must not be null value");
            this.name = name;
        }

        public static Builder create(final String attachmentName) {
            return new Builder(attachmentName);
        }

        public Builder setBody(final String body) {
            Objects.requireNonNull(body, "Body should not be null value");
            this.body = body;
            return this;
        }

        public Builder setStatus(final String status) {
            Objects.requireNonNull(status, "Status should not be null value");
            this.status = status;
            return this;
        }

        public GrpcResponseAttachment.Builder setMetadata(final String key, final String value) {
            Objects.requireNonNull(key, "Matadata key must not be null value");
            Objects.requireNonNull(value, "Matadata value must not be null value");
            this.metadata.put(key, value);
            return this;
        }

        public GrpcResponseAttachment.Builder addMetadata(final Map<String, String> metadata) {
            Objects.requireNonNull(metadata, "Metadata Map must not be null value");
            this.metadata.putAll(metadata);
            return this;
        }

        public GrpcResponseAttachment build() {
            return new GrpcResponseAttachment(name, body, status, metadata);
        }
    }
}
