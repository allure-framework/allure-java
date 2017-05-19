package io.qameta.allure.retrofit;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.httpattachment.HttpAttachment;
import io.qameta.allure.httpattachment.HttpAttachmentBuilder;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import okhttp3.Interceptor;
import okhttp3.Response;
import okhttp3.Connection;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.Buffer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.String.valueOf;

/**
 * Allure interceptor logger for Retrofit.
 */
public class AllureLoggingInterceptor implements Interceptor {

    private String templatePath;
    private final AllureLifecycle lifecycle;

    public AllureLoggingInterceptor() {
        this(Allure.getLifecycle());
    }

    public AllureLoggingInterceptor(final AllureLifecycle lifecycle) {
        this.templatePath = "/templates/default.ftl";
        this.lifecycle = lifecycle;
    }

    public AllureLoggingInterceptor withTemplate(final String templatePath) {
        this.templatePath = templatePath;
        return this;
    }

    @Override
    public Response intercept(final Chain chain) throws IOException {

        final Connection connection = chain.connection();
        final Request request = chain.request();
        final Response response = chain.proceed(request);
        final Response processedResponse = processResponse(response);
        final HttpAttachment httpAttachment = createHttpAttachment(request, connection, processedResponse);
        processAttachment(httpAttachment);
        return processedResponse;

    }

    private void addRequestBody(final HttpAttachment builder, final Request request) throws IOException {
        final RequestBody requestBody = request.body();
        if (requestBody != null && requestBody.contentLength() > 0) {
            builder.addRequestHeaders("content-length", valueOf(requestBody.contentLength()));
            builder.addRequestHeaders("content-type", requestBody.contentType().toString());
            final String body = readRequestBody(requestBody);
            builder.withRequestBody(body);
        }
    }

    private void addQueryParams(final HttpAttachment builder, final Request request) throws IOException {
        final Map<String, List<String>> requestParams = new HashMap<>();
        request.url().queryParameterNames().forEach(name ->
                requestParams.put(name, request.url().queryParameterValues(name)));
        builder.withQueryParams(toMapConverter(requestParams));
    }

    private Response processResponse(final Response response)
            throws IOException {
        Response result = null;
        final ResponseBody responseBody = response.body();
        if (responseBody != null) {
            final String responseBodyString = responseBody.string();
            final byte[] bytes = responseBodyString.getBytes(StandardCharsets.UTF_8);
            result = response.newBuilder()
                    .body(ResponseBody.create(responseBody.contentType(), bytes))
                    .build();
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

    protected HttpAttachment createHttpAttachment(final Request request, final Connection connection,
                                                  final Response response) throws IOException {
        final String url = request.url().toString();
        final String method = request.method();
        final Map<String, String> requestHeaders = toMapConverter(request.headers().toMultimap());
        final HttpAttachment httpAttachment = new HttpAttachment(method, url);
        final Protocol protocol = connection != null ? connection.protocol() : Protocol.HTTP_1_1;

        httpAttachment.withRequestHeaders(requestHeaders);
        addRequestBody(httpAttachment, request);
        addQueryParams(httpAttachment, request);

        final String version = protocol.toString().toUpperCase();
        final String status = String.format("%s %s %s", version, response.code(), response.message());
        final Map<String, List<String>> responseHeaders = response.headers().toMultimap();

        httpAttachment.withResponseStatus(status)
                .withResponseHeaders(toMapConverter(responseHeaders));
        final ResponseBody responseBody = response.body();
        if (responseBody != null) {
            httpAttachment.withResponseBody(response.body().string());
        }

        return httpAttachment;
    }

    protected void processAttachment(final HttpAttachment httpAttachment) {
        final HttpAttachmentBuilder allureHttpAttachmentBuilder = new HttpAttachmentBuilder(httpAttachment);
        final byte[] bytes = allureHttpAttachmentBuilder.buildFromTemplate(templatePath);

        final String uuid = UUID.randomUUID().toString();
        final StepResult stepResult = new StepResult()
                .withName(String.format("%s: %s", httpAttachment.getRequestMethod(),
                        httpAttachment.getRequestUrl())).withStatus(Status.PASSED);
        getLifecycle().startStep(uuid, stepResult);
        getLifecycle().addAttachment(httpAttachment.getResponseStatus(), "text/html", "md", bytes);
        getLifecycle().stopStep(uuid);
    }

    protected AllureLifecycle getLifecycle() {
        return lifecycle;
    }
}
