package io.qameta.allure.retrofit;

import com.google.common.base.Joiner;
import io.qameta.allure.curl.CurlBuilder;
import io.qameta.allure.curl.FreemarkerUtils;
import okhttp3.*;
import okio.Buffer;
import ru.yandex.qatools.allure.Allure;
import ru.yandex.qatools.allure.events.MakeAttachmentEvent;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.valueOf;

public class AllureLoggingInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        AllureData allureData = new AllureData();

        Connection connection = chain.connection();
        Protocol protocol = connection != null ? connection.protocol() : Protocol.HTTP_1_1;
        String version = protocol.toString().toUpperCase();

        Request request = chain.request();

        String url = request.url().toString();
        String method = request.method();
        Map<String, String> requestHeaders = toMapConverter(request.headers().toMultimap());

        allureData.setRequestUrl(url);
        allureData.setRequestMethod(method);
        allureData.setRequestHeaders(requestHeaders);

        RequestBody requestBody = request.body();
        String body = null;
        if (requestBody != null && requestBody.contentLength() > 0) {
            allureData.addRequestHeaders("content-length", valueOf(requestBody.contentLength()));
            allureData.addRequestHeaders("content-type", requestBody.contentType().toString());
            body = readRequestBody(requestBody);
            allureData.setRequestBody(body);
        }

        Map<String, List<String>> requestParams = new HashMap<>();
        request.url().queryParameterNames().forEach(name ->
                requestParams.put(name, request.url().queryParameterValues(name)));

        allureData.setQueryParams(toMapConverter(requestParams));

        Response response = chain.proceed(request);

        String status = String.format("%s %s %s", version, response.code(), response.message());
        Map<String, List<String>> responseHeaders = response.headers().toMultimap();

        allureData.setResponseStatus(status);
        allureData.setResponseHeaders(toMapConverter(responseHeaders));

        ResponseBody responseBody = response.body();
        if (responseBody != null) {
            String responseBodyString = responseBody.string();
            response = response.newBuilder()
                    .body(ResponseBody.create(responseBody.contentType(), responseBodyString.getBytes()))
                    .build();
            allureData.setResponseBody(responseBodyString);
        }

        allureData.setCurl(new CurlBuilder(method, url).header(requestHeaders).body(body).toString());

        byte[] bytes = FreemarkerUtils.process("report_api", allureData);
        Allure.LIFECYCLE.fire(new MakeAttachmentEvent(bytes, "Retrofit Log", "text/html"));

        return response;
    }

    private static String readRequestBody(RequestBody requestBody) throws IOException {
        Buffer buffer = new Buffer();
        requestBody.writeTo(buffer);
        Charset charset = requestBody.contentType().charset() == null ?
                Charset.forName("UTF-8") : requestBody.contentType().charset();
        return buffer.readString(charset);
    }

    private static Map<String, String> toMapConverter(Map<String, List<String>> items) {
        Map<String, String> result = new HashMap<>();
        items.forEach((k, l) -> result.put(k, Joiner.on("; ").join(l)));
        return result;
    }

}
