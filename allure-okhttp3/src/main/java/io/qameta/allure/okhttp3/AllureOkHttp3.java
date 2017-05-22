package io.qameta.allure.okhttp3;

import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.qameta.allure.attachment.http.HttpRequestAttachment.Builder.create;

/**
 * Allure interceptor logger for Retrofit.
 */
public class AllureOkHttp3 implements Interceptor {

    @SuppressWarnings("NullableProblems")
    @Override
    public Response intercept(final Chain chain) throws IOException {
        final Request request = chain.request();
        final HttpRequestAttachment.Builder builder = create("Request", request.url().toString())
                .headers(toMapConverter(request.headers().toMultimap()));

        final RequestBody requestBody = request.body();
        if (Objects.nonNull(requestBody)) {
            builder.body(readRequestBody(requestBody));
        }
        final HttpRequestAttachment requestAttachment = builder.build();
        new DefaultAttachmentProcessor().addAttachment(
                requestAttachment,
                new FreemarkerAttachmentRenderer("http-request.ftl")
        );
        return chain.proceed(request);

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
