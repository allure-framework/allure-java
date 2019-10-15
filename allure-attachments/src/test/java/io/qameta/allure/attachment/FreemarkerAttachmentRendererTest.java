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

import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.test.AllureFeatures;
import org.junit.jupiter.api.Test;

import static io.qameta.allure.attachment.testdata.TestData.randomHttpRequestAttachment;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
class FreemarkerAttachmentRendererTest {

    @AllureFeatures.Attachments
    @Test
    void shouldRenderRequestAttachment() {
        final HttpRequestAttachment data = randomHttpRequestAttachment();
        final DefaultAttachmentContent content = new FreemarkerAttachmentRenderer("http-request.ftl")
                .render(data);

        assertThat(content)
                .hasFieldOrPropertyWithValue("contentType", "text/html")
                .hasFieldOrPropertyWithValue("fileExtension", ".html")
                .hasFieldOrProperty("content");
    }

    @Test
    void shouldRenderRequestTimeAttachment() {
        final HttpRequestAttachment data = randomHttpRequestAttachment();
        final DefaultAttachmentContent content = new FreemarkerAttachmentRenderer("http-request.ftl")
                .render(data);

        assertThat(content.getContent()).contains("Request time");
    }

    @AllureFeatures.Attachments
    @Test
    void shouldRenderResponseAttachment() {
        final HttpRequestAttachment data = randomHttpRequestAttachment();
        final DefaultAttachmentContent content = new FreemarkerAttachmentRenderer("http-response.ftl")
                .render(data);

        assertThat(content)
                .hasFieldOrPropertyWithValue("contentType", "text/html")
                .hasFieldOrPropertyWithValue("fileExtension", ".html")
                .hasFieldOrProperty("content");
    }
}
