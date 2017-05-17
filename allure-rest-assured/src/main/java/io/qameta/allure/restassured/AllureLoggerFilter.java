package io.qameta.allure.restassured;

import io.qameta.allure.httpattachment.AllureHttpAttachmentBuilder;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.internal.NameAndValue;
import io.restassured.internal.support.Prettifier;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.util.HashMap;
import java.util.Map;

/**
 * Allure logger filter for Rest-assured.
 */
public class AllureLoggerFilter implements OrderedFilter {

    private String templatePath;

    public AllureLoggerFilter withTemplate(final String templatePath) {
        this.templatePath = templatePath;
        return this;
    }

    @Override
    public Response filter(final FilterableRequestSpecification requestSpec,
                           final FilterableResponseSpecification responseSpec,
                           final FilterContext filterContext) {


        final Prettifier prettifier = new Prettifier();
        final Response response = filterContext.next(requestSpec, responseSpec);
        final AllureHttpAttachmentBuilder allureHttpAttachmentBuilder =
                new AllureHttpAttachmentBuilder(requestSpec.getMethod(), requestSpec.getURI());

        allureHttpAttachmentBuilder.withQueryParams(requestSpec.getQueryParams())
                .withRequestBody(prettifier.getPrettifiedBodyIfPossible(requestSpec))
                .withRequestHeaders(toMapConverter(requestSpec.getHeaders()))
                .withRequestCookies(toMapConverter(requestSpec.getCookies()))
                .withResponseStatus(response.getStatusLine())
                .withResponseHeaders(toMapConverter(response.getHeaders()))
                .withResponseBody(prettifier.getPrettifiedBodyIfPossible(response, response.getBody()));

        if (templatePath == null) {
            allureHttpAttachmentBuilder.build();
        } else {
            allureHttpAttachmentBuilder.build(templatePath);
        }
        return response;
    }

    private static Map<String, String> toMapConverter(final Iterable<? extends NameAndValue> items) {
        final Map<String, String> result = new HashMap<>();
        items.forEach(h -> result.put(h.getName(), h.getValue()));
        return result;
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }
}
