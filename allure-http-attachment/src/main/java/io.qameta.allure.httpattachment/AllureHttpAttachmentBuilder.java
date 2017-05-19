package io.qameta.allure.httpattachment;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

import static org.apache.commons.io.FilenameUtils.getFullPathNoEndSeparator;
import static org.apache.commons.io.FilenameUtils.getName;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

/**
 * Build attachment for allure report.
 */
public class AllureHttpAttachmentBuilder {

    private static final String DEFAULT_TEMPLATE_PATH = "/templates/default.ftl";
    private final AllureHttpAttachmentData data;

    public AllureHttpAttachmentBuilder(final String requestMethod, final String requestUrl) {
        this.data = new AllureHttpAttachmentData();
        data.setRequestUrl(escapeHtml4(requestUrl));
        data.setRequestMethod(escapeHtml4(requestMethod));
    }

    public AllureHttpAttachmentBuilder withQueryParams(final Map<String, String> queryParams) {
        queryParams.forEach(escape());
        data.setQueryParams(queryParams);
        return this;
    }

    public AllureHttpAttachmentBuilder withRequestHeaders(final Map<String, String> requestHeaders) {
        requestHeaders.forEach(escape());
        data.setRequestHeaders(requestHeaders);
        return this;
    }

    public AllureHttpAttachmentBuilder withRequestCookies(final Map<String, String> requestCookies) {
        requestCookies.forEach(escape());
        data.setRequestCookies(requestCookies);
        return this;
    }

    public AllureHttpAttachmentBuilder withRequestBody(final String requestBody) {
        data.setRequestBody(escapeHtml4(requestBody));
        return this;
    }

    public AllureHttpAttachmentBuilder withResponseStatus(final String responseStatus) {
        data.setResponseStatus(escapeHtml4(responseStatus));
        return this;
    }

    public AllureHttpAttachmentBuilder withResponseHeaders(final Map<String, String> responseHeaders) {
        responseHeaders.forEach(escape());
        data.setResponseHeaders(responseHeaders);
        return this;
    }

    public AllureHttpAttachmentBuilder withResponseBody(final String responseBody) {
        data.setResponseBody(escapeHtml4(responseBody));
        return this;
    }

    public void addRequestHeaders(final String name, final String value) {
        data.addRequestHeaders(escapeHtml4(name), escapeHtml4(value));
    }

    private String generateCurl() {
        return new CurlBuilder(data.getRequestMethod(), data.getRequestUrl())
                .cookie(data.getRequestCookies()).header(data.getRequestHeaders())
                .body(data.getRequestBody()).toString();
    }

    private static BiConsumer<String, String> escape() {
        return (k, v) -> {
            escapeHtml4(k);
            escapeHtml4(v);
        };
    }

    public void build() {
        build(DEFAULT_TEMPLATE_PATH);
    }

    public void build(final String templatePath) {
        data.setCurl(generateCurl());
        final byte[] bytes = process(templatePath, data);
        final AllureLifecycle lifecycle = Allure.getLifecycle();
        final String uuid = UUID.randomUUID().toString();
        final StepResult stepResult = new StepResult()
                .withName(String.format("%s: %s", data.getRequestMethod(), data.getRequestUrl()))
                .withStatus(Status.PASSED);
        lifecycle.startStep(uuid, stepResult);
        lifecycle.addAttachment("Api report Log", "text/html", "md", bytes);
        lifecycle.stopStep(uuid);
    }

    private static byte[] process(final String templatePath, final Object object) {
        final String packagePath = getFullPathNoEndSeparator(templatePath);
        final String displayName = getName(templatePath);
        final Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
        cfg.setClassForTemplateLoading(AllureHttpAttachmentBuilder.class, packagePath);
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            final Writer writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
            final Template template = cfg.getTemplate(displayName);
            template.process(object, writer);
            return stream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Can't read template " + templatePath, e);
        } catch (TemplateException e) {
            throw new IllegalStateException("Can't process template " + templatePath, e);
        }
    }
}
