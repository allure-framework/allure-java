/*
 *  Copyright 2016-2024 Qameta Software Inc
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

import java.util.Objects;

/**
 * @author dtuchs (Dmitrii Tuchs).
 */
public class GrpcRequestAttachment implements AttachmentData {

    private final String name;
    private final String url;
    private final String body;

    public GrpcRequestAttachment(final String name, final String url, final String body) {
        this.name = name;
        this.url = url;
        this.body = body;
    }

    public String getUrl() {
        return url;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Builder for GrpcRequestAttachment.
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    static final class Builder {

        private final String name;

        private final String url;

        private String body;

        private Builder(final String name, final String url) {
            Objects.requireNonNull(name, "Name must not be null value");
            Objects.requireNonNull(url, "Url must not be null value");
            this.name = name;
            this.url = url;
        }

        public static Builder create(final String attachmentName, final String url) {
            return new Builder(attachmentName, url);
        }

        public Builder setBody(final String body) {
            Objects.requireNonNull(body, "Body should not be null value");
            this.body = body;
            return this;
        }

        public GrpcRequestAttachment build() {
            return new GrpcRequestAttachment(name, url, body);
        }
    }
}
