package io.qameta.allure.okhttp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import io.qameta.allure.attachment.AttachmentData;
import io.qameta.allure.attachment.AttachmentProcessor;
import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.attachment.http.HttpResponseAttachment;
import okio.Buffer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Allure interceptor logger for OkHttp.
 */
public class AllureOkHttp implements Interceptor {

    private String requestTemplatePath = "http-request.ftl";
    private String responseTemplatePath = "http-response.ftl";
    private boolean prettyPrintJson = false;

    public AllureOkHttp withRequestTemplate(final String templatePath) {
        this.requestTemplatePath = templatePath;
        return this;
    }

    public AllureOkHttp withResponseTemplate(final String templatePath) {
        this.responseTemplatePath = templatePath;
        return this;
    }

    public AllureOkHttp withJsonPrettyPrint(final boolean prettyPrintJson) {
        this.prettyPrintJson = prettyPrintJson;
        return this;
    }

    @Override
    public Response intercept(final Chain chain) throws IOException {
        final AttachmentProcessor<AttachmentData> processor = new DefaultAttachmentProcessor();

        final Request request = chain.request();
        final String requestUrl = request.url().toString();
        final HttpRequestAttachment.Builder requestAttachmentBuilder = HttpRequestAttachment.Builder
            .create("Request", requestUrl).withHeaders(toMapConverter(request.headers().toMultimap()));

        final RequestBody requestBody = request.body();
        if (Objects.nonNull(requestBody)) {
            requestAttachmentBuilder.withBody(readRequestBody(requestBody));
        }
        final HttpRequestAttachment requestAttachment = requestAttachmentBuilder.build();
        processor.addAttachment(requestAttachment, new FreemarkerAttachmentRenderer(requestTemplatePath));

        final Response response = chain.proceed(request);
        final HttpResponseAttachment.Builder responseAttachmentBuilder = HttpResponseAttachment.Builder
            .create("Response").withResponseCode(response.code())
            .withHeaders(toMapConverter(response.headers().toMultimap()));

        final Response.Builder responseBuilder = response.newBuilder();

        final ResponseBody responseBody = response.body();

        if (Objects.nonNull(responseBody)) {
            final byte[] bytes = responseBody.bytes();
            responseAttachmentBuilder.withBody(readResponseBody(bytes));
            responseBuilder.body(ResponseBody.create(responseBody.contentType(), bytes));
        }

        final HttpResponseAttachment responseAttachment = responseAttachmentBuilder.build();
        processor.addAttachment(responseAttachment, new FreemarkerAttachmentRenderer(responseTemplatePath));

        return responseBuilder.build();
    }

    private Map<String, String> toMapConverter(final Map<String, List<String>> items) {
        final Map<String, String> result = new HashMap<>();
        items.forEach((key, value) -> result.put(key, String.join("; ", value)));
        return result;
    }

    private String readRequestBody(final RequestBody requestBody) throws IOException {
        final Buffer buffer = new Buffer();
        requestBody.writeTo(buffer);
        final String requestString = buffer.readString(StandardCharsets.UTF_8);
        if (this.prettyPrintJson) {
            return prettyJson(requestString);
        }
        return requestString;
    }

    private String readResponseBody(final byte[] bytes) {
        final String responseString = new String(bytes, StandardCharsets.UTF_8);
        if (prettyPrintJson) {
            return prettyJson(responseString);
        }
        return responseString;
    }

    private String prettyJson(String json) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            JsonElement parse = new JsonParser().parse(json);
            return gson.toJson(parse);
        } catch (JsonSyntaxException e) {
            return json;
        }
    }
}
