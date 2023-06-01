/*
 *  Copyright 2019 Qameta Software OÜ
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.attachment;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;

/**
 * @author charlie (Dmitry Baev).
 */
public class FreemarkerAttachmentRenderer implements AttachmentRenderer<AttachmentData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreemarkerAttachmentRenderer.class);

    private final Configuration configuration;

    private final String templateName;

    public FreemarkerAttachmentRenderer(final String templateName) {
        this.templateName = templateName;
        this.configuration = new Configuration(Configuration.VERSION_2_3_23);
        this.configuration.setLocalizedLookup(false);
        this.configuration.setTemplateUpdateDelayMilliseconds(0);
        this.configuration.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        this.configuration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "tpl");
    }

    @Override
    public DefaultAttachmentContent render(final AttachmentData data) {
        try (Writer writer = new StringWriter()) {
            final Template template = configuration.getTemplate(templateName);
            template.process(Collections.singletonMap("data", data), writer);
            return new DefaultAttachmentContent(writer.toString(), "text/html", ".html");
        } catch (Exception e) {
            LOGGER.debug(data.toString());
            throw new AttachmentRenderException("Could't render http attachment file", e);
        }
    }

}
