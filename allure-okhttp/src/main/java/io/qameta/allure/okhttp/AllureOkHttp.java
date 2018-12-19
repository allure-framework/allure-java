package io.qameta.allure.okhttp;

import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.attachment.http.HttpResponseAttachment;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import okio.Buffer;

/**
 * Allure interceptor logger for OkHttp.
 */
public class AllureOkHttp implements Interceptor {

    private String requestTemplatePath = "http-request.ftl";
    private String responseTemplatePath = "http-response.ftl";

    public AllureOkHttp setRequestTemplate(final String templatePath) {
        this.requestTemplatePath = templatePath;
        return this;
    }

    public AllureOkHttp setResponseTemplate(final String templatePath) {
        this.responseTemplatePath = templatePath;
        return this;
    }

    /**
     * @deprecated use {@link #setRequestTemplate(String)} instead.
     * Scheduled for removal in 3.0 release.
     */
    @Deprecated
    public AllureOkHttp withRequestTemplate(final String templatePath) {
        return setRequestTemplate(templatePath);
    }

    /**
     * @deprecated use {@link #setResponseTemplate(String)} instead.
     * Scheduled for removal in 3.0 release.
     */
    @Deprecated
    public AllureOkHttp withResponseTemplate(final String templatePath) {
        return setResponseTemplate(templatePath);
    }

    @Override
    public Response intercept(final Chain chain) throws IOException {
        final AttachmentProcessor<AttachmentData> processor = new DefaultAttachmentProcessor();

        final Request request = chain.request();
        final String requestUrl = request.url().toString();
        final HttpRequestAttachment.Builder requestAttachmentBuilder = HttpRequestAttachment.Builder
                .create("Request", requestUrl)
                .setMethod(request.method())
                .setHeaders(toMapConverter(request.headers().toMultimap()));

        final RequestBody requestBody = request.body();
        if (Objects.nonNull(requestBody)) {
            requestAttachmentBuilder.setBody(readRequestBody(requestBody));
        }
        final HttpRequestAttachment requestAttachment = requestAttachmentBuilder.build();
        processor.addAttachment(requestAttachment, new FreemarkerAttachmentRenderer(requestTemplatePath));

        final Response response = chain.proceed(request);
        final HttpResponseAttachment.Builder responseAttachmentBuilder = HttpResponseAttachment.Builder
                .create("Response")
                .setResponseCode(response.code())
                .setHeaders(toMapConverter(response.headers().toMultimap()));

        final Response.Builder responseBuilder = response.newBuilder();

        final ResponseBody responseBody = response.body();

        if (Objects.nonNull(responseBody)) {
            final byte[] bytes = responseBody.bytes();
            responseAttachmentBuilder.setBody(new String(bytes, StandardCharsets.UTF_8));
            responseBuilder.body(ResponseBody.create(responseBody.contentType(), bytes));
        }

        final HttpResponseAttachment responseAttachment = responseAttachmentBuilder.build();
        processor.addAttachment(responseAttachment, new FreemarkerAttachmentRenderer(responseTemplatePath));

        return responseBuilder.build();
    }

    private static Map<String, String> toMapConverter(final Map<String, List<String>> items) {
        final Map<String, String> result = new HashMap<>();
        items.forEach((key, value) -> result.put(key, String.join("; ", value)));
        return result;
    }

    private static String readRequestBody(final RequestBody requestBody) throws IOException {
        final Buffer buffer = new Buffer();
        requestBody.writeTo(buffer);
        return buffer.readString(StandardCharsets.UTF_8);
    }
}
