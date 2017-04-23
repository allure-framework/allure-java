package io.qameta.allure.curl;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class FreemarkerUtils {

    private FreemarkerUtils() {
    }

    public static byte[] process(String templateName, Object object) {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_23);
        cfg.setClassForTemplateLoading(FreemarkerUtils.class, "/templates");
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            Writer writer = new OutputStreamWriter(stream);
            Template template = cfg.getTemplate(String.format("%s.ftl", templateName));
            template.process(object, writer);
            return stream.toByteArray();
        } catch (IOException | TemplateException e) {
            throw new RuntimeException(e);
        }
    }
}
