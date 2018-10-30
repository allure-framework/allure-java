package io.qameta.allure.attachment;

import freemarker.template.Configuration;
import freemarker.template.Template;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;

/**
 * @author charlie (Dmitry Baev).
 */
public class FreemarkerAttachmentRenderer implements AttachmentRenderer<AttachmentData> {

    private final Configuration configuration;

    private final String templateName;

    public FreemarkerAttachmentRenderer(final String templateName) {
        this.templateName = templateName;
        this.configuration = new Configuration(Configuration.VERSION_2_3_23);
        this.configuration.setLocalizedLookup(false);
        this.configuration.setTemplateUpdateDelayMilliseconds(0);
        this.configuration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "tpl");
    }

    @Override
    public DefaultAttachmentContent render(final AttachmentData data) {
        try (Writer writer = new StringWriter()) {
            final Template template = configuration.getTemplate(templateName);
            template.process(Collections.singletonMap("data", data), writer);
            return new DefaultAttachmentContent(writer.toString(), "text/html", ".html");
        } catch (Exception e) {
            throw new AttachmentRenderException("Could't render http attachment file", e);
        }
    }

}
