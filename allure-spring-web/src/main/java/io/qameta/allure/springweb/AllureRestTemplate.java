package io.qameta.allure.springweb;

import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.attachment.http.HttpResponseAttachment;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * * Created by v.o.pavlov on 05.02.2019.
 */
public class AllureRestTemplate implements ClientHttpRequestInterceptor {

    private String requestTemplatePath = "http-request.ftl";
    private String responseTemplatePath = "http-response.ftl";

    public AllureRestTemplate setRequestTemplate(final String templatePath) {
        this.requestTemplatePath = templatePath;
        return this;
    }

    public AllureRestTemplate setResponseTemplate(final String templatePath) {
        this.responseTemplatePath = templatePath;
        return this;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public ClientHttpResponse intercept(@NonNull HttpRequest request, byte[] body, @NonNull ClientHttpRequestExecution execution) throws IOException {
        final AttachmentProcessor<AttachmentData> processor = new DefaultAttachmentProcessor();

        final HttpRequestAttachment.Builder requestAttachmentBuilder = HttpRequestAttachment.Builder
                .create("Request", request.getURI().toString())
                .setMethod(request.getMethodValue())
                .setHeaders(toMapConverter(request.getHeaders()));
        if (body.length != 0) {
            requestAttachmentBuilder.setBody(new String(body, StandardCharsets.UTF_8));
        }

        final HttpRequestAttachment requestAttachment = requestAttachmentBuilder.build();
        processor.addAttachment(requestAttachment, new FreemarkerAttachmentRenderer(requestTemplatePath));

        ClientHttpResponse clientHttpResponse = execution.execute(request, body);

        final HttpResponseAttachment responseAttachment = HttpResponseAttachment.Builder
                .create("Response")
                .setResponseCode(clientHttpResponse.getRawStatusCode())
                .setHeaders(toMapConverter(clientHttpResponse.getHeaders()))
                .setBody(new String(StreamUtils.copyToByteArray(clientHttpResponse.getBody())))
                .build();
        processor.addAttachment(responseAttachment, new FreemarkerAttachmentRenderer(responseTemplatePath));

        return clientHttpResponse;
    }

    private static Map<String, String> toMapConverter(final Map<String, List<String>> items) {
        final Map<String, String> result = new HashMap<>();
        items.forEach((key, value) -> result.put(key, String.join("; ", value)));
        return result;
    }
}
