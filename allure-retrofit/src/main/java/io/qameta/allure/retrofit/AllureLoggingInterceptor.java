package io.qameta.allure.retrofit;

import io.qameta.allure.http_attachment.AllureHttpAttachmentBuilder;
import okhttp3.*;
import okio.Buffer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.valueOf;

/**
 * Allure interceptor logger for Retrofit.
 */
public class AllureLoggingInterceptor implements Interceptor {

    private String templatePath;

    public AllureLoggingInterceptor withTemplate(final String templatePath) {
        this.templatePath = templatePath;
        return this;
    }

    @Override
    public Response intercept(final Chain chain) throws IOException {

        final Connection connection = chain.connection();
        final Protocol protocol = connection != null ? connection.protocol() : Protocol.HTTP_1_1;

        final Request request = chain.request();
        final String url = request.url().toString();
        final String method = request.method();
        final Map<String, String> requestHeaders = toMapConverter(request.headers().toMultimap());

        final AllureHttpAttachmentBuilder allureHttpAttachmentBuilder = new AllureHttpAttachmentBuilder(method, url);
        allureHttpAttachmentBuilder.withRequestHeaders(requestHeaders);
        addRequestBody(allureHttpAttachmentBuilder, request);
        addQueryParams(allureHttpAttachmentBuilder, request);
        Response response = chain.proceed(request);
        response = addResponse(allureHttpAttachmentBuilder, response, protocol);
        if (templatePath == null) {
            allureHttpAttachmentBuilder.build();
        } else {
            allureHttpAttachmentBuilder.build(templatePath);
        }

        return response;
    }

    private void addRequestBody(final AllureHttpAttachmentBuilder builder, final Request request) throws IOException {
        final RequestBody requestBody = request.body();
        if (requestBody != null && requestBody.contentLength() > 0) {
            builder.addRequestHeaders("content-length", valueOf(requestBody.contentLength()));
            builder.addRequestHeaders("content-type", requestBody.contentType().toString());
            final String body = readRequestBody(requestBody);
            builder.withRequestBody(body);
        }
    }

    private void addQueryParams(final AllureHttpAttachmentBuilder builder, final Request request) throws IOException {
        final Map<String, List<String>> requestParams = new HashMap<>();
        request.url().queryParameterNames().forEach(name ->
                requestParams.put(name, request.url().queryParameterValues(name)));
        builder.withQueryParams(toMapConverter(requestParams));
    }

    private Response addResponse(final AllureHttpAttachmentBuilder builder, final Response response,
                                 final Protocol protocol)
            throws IOException {
        final String version = protocol.toString().toUpperCase();
        final String status = String.format("%s %s %s", version, response.code(), response.message());
        final Map<String, List<String>> responseHeaders = response.headers().toMultimap();

        builder.withResponseStatus(status)
                .withResponseHeaders(toMapConverter(responseHeaders));

        Response result = null;
        final ResponseBody responseBody = response.body();
        if (responseBody != null) {
            final String responseBodyString = responseBody.string();
            final byte[] bytes = responseBodyString.getBytes(StandardCharsets.UTF_8);
            result = response.newBuilder()
                    .body(ResponseBody.create(responseBody.contentType(), bytes))
                    .build();
            builder.withResponseBody(responseBodyString);
        }
        return result;
    }

    private static String readRequestBody(final RequestBody requestBody) throws IOException {
        final Buffer buffer = new Buffer();
        requestBody.writeTo(buffer);
        return buffer.readString(StandardCharsets.UTF_8);
    }

    private static Map<String, String> toMapConverter(final Map<String, List<String>> items) {
        final Map<String, String> result = new HashMap<>();
        items.forEach((k, l) -> result.put(k, String.join("; ", l)));
        return result;
    }

}
