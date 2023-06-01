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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static io.qameta.allure.attachment.testdata.TestData.negativeHttpRequestAttachment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author a-simeshin (Simeshin Artem).
 */
class NegativeFreemarkerAttachmentRendererTest {

    private static final String TEMPLATE_FOR_EXCEPTION = "body-npe-non-safe-attachment.ftl";

    private PrintStream realSysOut;
    private ByteArrayOutputStream sysOutBuffer;

    @BeforeEach
    void setUpSysOut() throws UnsupportedEncodingException {
        realSysOut = System.err;
        sysOutBuffer = new ByteArrayOutputStream();
        System.setErr(new PrintStream(sysOutBuffer, false, StandardCharsets.UTF_8.toString()));
    }

    @AfterEach
    void rollBackSysOut() {
        System.setErr(realSysOut);
    }

    @AllureFeatures.Attachments
    @Test
    void shouldThrowExceptionalSituationsForFreeMarketRendererWithIncorrectAttachmentData() {
        assertThrows(AttachmentRenderException.class, () -> {
            final HttpRequestAttachment data = negativeHttpRequestAttachment();
            new FreemarkerAttachmentRenderer(TEMPLATE_FOR_EXCEPTION).render(data);
        });
    }

    @AllureFeatures.Attachments
    @Test
    void shouldExplainExceptionalSituationsForFreeMarketRenderer() throws UnsupportedEncodingException {
        try {
            final HttpRequestAttachment data = negativeHttpRequestAttachment();
            new FreemarkerAttachmentRenderer(TEMPLATE_FOR_EXCEPTION).render(data);
        } catch (Exception ignored) {
            // for test purposes
        }
        assertThat(sysOutBuffer.toString(StandardCharsets.UTF_8.toString()))
                .contains("SEVERE: Error executing FreeMarker template")
                .contains("FreeMarker template error:")
                .contains("The following has evaluated to null or missing:")
                .contains("==> data.body")
                .contains("[in template \"body-npe-non-safe-attachment.ftl\" at line 8, column 11]")
                .contains("\t- Failed at: ${data.body.size}")
                .contains("io.qameta.allure.attachment.FreemarkerAttachmentRenderer - HttpRequestAttachment")
                .contains("\tname=null,")
                .contains("\turl=null,")
                .contains("\tbody=null,")
                .contains("\theaders={},")
                .contains("\tcookies={}");
    }
}
