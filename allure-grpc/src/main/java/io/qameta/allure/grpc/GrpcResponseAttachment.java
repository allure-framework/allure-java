package io.qameta.allure.grpc;

import io.qameta.allure.attachment.AttachmentData;

import java.util.Objects;

public class GrpcResponseAttachment implements AttachmentData {

    private final String name;
    private final String body;

    public GrpcResponseAttachment(String name, String body) {
        this.name = name;
        this.body = body;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Builder for GrpcResponseAttachment.
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    static final class Builder {

        private final String name;

        private String body;

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

        public GrpcResponseAttachment build() {
            return new GrpcResponseAttachment(name, body);
        }
    }
}
