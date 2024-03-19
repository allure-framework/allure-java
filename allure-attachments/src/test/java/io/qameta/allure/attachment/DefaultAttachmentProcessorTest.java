/*
 *  Copyright 2016-2024 Qameta Software Inc
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
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.test.AllureFeatures;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static io.qameta.allure.attachment.testdata.TestData.randomAttachmentContent;
import static io.qameta.allure.attachment.testdata.TestData.randomHttpRequestAttachment;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author charlie (Dmitry Baev).
 */
class DefaultAttachmentProcessorTest {

    @AllureFeatures.Attachments
    @Test
    void shouldProcessAttachments() {
        final HttpRequestAttachment attachment = randomHttpRequestAttachment();
        final AllureLifecycle lifecycle = mock(AllureLifecycle.class);
        final AttachmentRenderer<AttachmentData> renderer = mock();
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

    @AllureFeatures.Attachments
    @Test
    void shouldProcessWrapAttachmentsWithMetaSteps() {
        final HttpRequestAttachment attachment = randomHttpRequestAttachment();
        final AllureLifecycle lifecycle = mock(AllureLifecycle.class);
        final AttachmentRenderer<AttachmentData> renderer = mock();
        final AttachmentContent content = randomAttachmentContent();
        doReturn(content)
                .when(renderer)
                .render(attachment);

        new DefaultAttachmentProcessor(lifecycle)
                .addAttachment(attachment, renderer);

        verify(renderer, times(1)).render(attachment);

        final ArgumentCaptor<String> stepUuidCaptor = ArgumentCaptor.captor();

        verify(lifecycle, times(1))
                .startStep(
                        stepUuidCaptor.capture(),
                        eq(new StepResult().setName(attachment.getName()))
                );

        verify(lifecycle, times(1))
                .addAttachment(
                        eq(attachment.getName()),
                        eq(content.getContentType()),
                        eq(content.getFileExtension()),
                        eq(content.getContent().getBytes(StandardCharsets.UTF_8))
                );

        final ArgumentCaptor<Consumer<StepResult>> consumerArgumentCaptor = ArgumentCaptor.captor();

        verify(lifecycle, times(1))
                .updateStep(
                        eq(stepUuidCaptor.getValue()),
                        consumerArgumentCaptor.capture()
                );

        final StepResult stepResultCheck = mock();

        doReturn(stepResultCheck)
                .when(stepResultCheck)
                .setStatus(any());

        consumerArgumentCaptor.getValue().accept(stepResultCheck);

        verify(stepResultCheck, times(1))
                .setStatus(Status.PASSED);

        verify(lifecycle, times(1))
                .stopStep(
                        eq(stepUuidCaptor.getValue())
                );
    }

}
