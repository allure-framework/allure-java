package io.qameta.allure.grpc;

import io.qameta.allure.attachment.AttachmentData;

import java.util.Objects;

public class GrpcRequestAttachment implements AttachmentData {

    private final String name;
    private final String url;
    private final String body;

    public GrpcRequestAttachment(String name, String url, String body) {
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
