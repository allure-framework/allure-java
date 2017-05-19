package io.qameta.allure.httpattachment;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import static org.apache.commons.io.FilenameUtils.getFullPathNoEndSeparator;
import static org.apache.commons.io.FilenameUtils.getName;

/**
 * Build attachment for allure report.
 */
public class HttpAttachmentBuilder {

    private HttpAttachment data;

    public HttpAttachmentBuilder(HttpAttachment data) {
        this.data = data;
    }

    public byte[] buildFromTemplate(final String templatePath) {
        return process(templatePath, data);
    }

    private static byte[] process(final String templatePath, final Object object) {
        final String packagePath = getFullPathNoEndSeparator(templatePath);
        final String displayName = getName(templatePath);
        final Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
        cfg.setClassForTemplateLoading(HttpAttachmentBuilder.class, packagePath);
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
