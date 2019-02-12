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

import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.test.AllureFeatures;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static io.qameta.allure.attachment.testdata.TestData.randomAttachmentContent;
import static io.qameta.allure.attachment.testdata.TestData.randomHttpRequestAttachment;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author charlie (Dmitry Baev).
 */
class DefaultAttachmentProcessorTest {

    @SuppressWarnings("unchecked")
    @AllureFeatures.Attachments
    @Test
    void shouldProcessAttachments() {
        final HttpRequestAttachment attachment = randomHttpRequestAttachment();
        final AllureLifecycle lifecycle = mock(AllureLifecycle.class);
        final AttachmentRenderer<AttachmentData> renderer = mock(AttachmentRenderer.class);
        final AttachmentContent content = randomAttachmentContent();
        doReturn(content)
                .when(renderer)
                .render(attachment);

        new DefaultAttachmentProcessor(lifecycle)
                .addAttachment(attachment, renderer);

        verify(renderer, times(1)).render(attachment);
        verify(lifecycle, times(1))
                .addAttachment(
                        eq(attachment.getName()),
                        eq(content.getContentType()),
                        eq(content.getFileExtension()),
                        eq(content.getContent().getBytes(StandardCharsets.UTF_8))
                );
    }
}
