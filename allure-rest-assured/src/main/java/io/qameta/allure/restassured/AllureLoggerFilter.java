package io.qameta.allure.restassured;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.httpattachment.HttpAttachmentBuilder;
import io.qameta.allure.httpattachment.HttpAttachment;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.internal.NameAndValue;
import io.restassured.internal.support.Prettifier;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Allure logger filter for Rest-assured.
 */
public class AllureLoggerFilter implements OrderedFilter {

    private String templatePath;

    private final AllureLifecycle lifecycle;

    public AllureLoggerFilter(){
        this(Allure.getLifecycle());
    }

    public AllureLoggerFilter(final AllureLifecycle lifecycle) {
        this.templatePath = "/templates/default.ftl";
        this.lifecycle = lifecycle;
    }

    public AllureLoggerFilter withTemplate(final String templatePath) {
        this.templatePath = templatePath;
        return this;
    }

    @Override
    public Response filter(final FilterableRequestSpecification requestSpec,
                           final FilterableResponseSpecification responseSpec,
                           final FilterContext filterContext) {
        final Response response = filterContext.next(requestSpec, responseSpec);
        HttpAttachment httpAttachment = createHttpAttachment(requestSpec, response);
        processAttachment(httpAttachment);
        return response;
    }

    protected HttpAttachment createHttpAttachment(final FilterableRequestSpecification requestSpec,
                                                  Response response) {
        final Prettifier prettifier = new Prettifier();
        HttpAttachment httpAttachment =
                new HttpAttachment(requestSpec.getMethod(), requestSpec.getURI());
        return httpAttachment.withQueryParams(requestSpec.getQueryParams())
                .withRequestBody(prettifier.getPrettifiedBodyIfPossible(requestSpec))
                .withRequestHeaders(toMapConverter(requestSpec.getHeaders()))
                .withRequestCookies(toMapConverter(requestSpec.getCookies()))
                .withResponseStatus(response.getStatusLine())
                .withResponseHeaders(toMapConverter(response.getHeaders()))
                .withResponseBody(prettifier.getPrettifiedBodyIfPossible(response, response.getBody()));
    }

    protected void processAttachment(HttpAttachment httpAttachment) {
        final HttpAttachmentBuilder allureHttpAttachmentBuilder = new HttpAttachmentBuilder(httpAttachment);
        byte[] bytes = allureHttpAttachmentBuilder.buildFromTemplate(templatePath);

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
