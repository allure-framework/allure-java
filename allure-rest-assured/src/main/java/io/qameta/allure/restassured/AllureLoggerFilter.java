package io.qameta.allure.restassured;

import io.qameta.allure.curl.CurlBuilder;
import io.qameta.allure.curl.FreemarkerUtils;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.internal.NameAndValue;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import ru.yandex.qatools.allure.Allure;
import ru.yandex.qatools.allure.events.MakeAttachmentEvent;
import ru.yandex.qatools.allure.events.StepFinishedEvent;
import ru.yandex.qatools.allure.events.StepStartedEvent;

import java.util.HashMap;
import java.util.Map;

public class AllureLoggerFilter implements Filter {

    public static AllureLoggerFilter log() {
        return new AllureLoggerFilter();
    }

    @Override
    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec,
                           FilterContext filterContext) {

        AllureData allureData = new AllureData();
        Response response = filterContext.next(requestSpec, responseSpec);

        String uri = requestSpec.getURI();
        String method = requestSpec.getMethod();
        String requestBody = requestSpec.getBody();

        allureData.setRequestUrl(uri);
        allureData.setRequestMethod(method);
        allureData.setRequestBody(requestBody);

        Map<String, String> requestCookies = toMapConverter(requestSpec.getCookies());
        Map<String, String> requestHeaders = toMapConverter(requestSpec.getHeaders());

        allureData.setQueryParams(requestSpec.getQueryParams());
        allureData.setRequestHeaders(requestHeaders);
        allureData.setRequestCookies(requestCookies);

        allureData.setResponseStatus(response.getStatusLine());
        allureData.setResponseHeaders(toMapConverter(response.getHeaders()));
        allureData.setResponseBody(response.getBody().prettyPeek().asString());

        allureData.setCurl(new CurlBuilder(method, uri)
                .cookie(requestCookies)
                .header(requestHeaders).body(requestBody).toString());

        byte[] bytes = FreemarkerUtils.process("report_api", allureData);
        Allure.LIFECYCLE.fire(new StepStartedEvent(String.format("%s: %s", requestSpec.getMethod(),
                requestSpec.getURI())));
        Allure.LIFECYCLE.fire(new MakeAttachmentEvent(bytes, "Rest-assured Log", "text/html"));
        Allure.LIFECYCLE.fire(new StepFinishedEvent());
        return response;
    }

    private static Map<String, String> toMapConverter(Iterable<? extends NameAndValue> items) {
        Map<String, String> result = new HashMap<>();
        items.forEach(h -> result.put(h.getName(), h.getValue()));
        return result;
    }
}
