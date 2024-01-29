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
package io.qameta.allure.aspects;

import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import org.junit.jupiter.api.Test;
import ru.yandex.qatools.allure.annotations.Attachment;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * eroshenkoam
 * 30.04.17
 */
class Allure1AttachAspectsTest {

    @Test
    void shouldSetupAttachmentTitleFromAnnotation() {
        final AllureResults results = runWithinTestContext(
                () -> attachmentWithTitleAndType("parameter value"),
                Allure1AttachAspects::setLifecycle
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getAttachments)
                .extracting("name", "type")
                .containsExactly(tuple("attachment with parameter value", "text/plain"));

    }

    @Test
    void shouldSetupAttachmentTitleFromMethodSignature() {
        final AllureResults results = runWithinTestContext(
                this::attachmentWithoutTitle,
                Allure1AttachAspects::setLifecycle
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getAttachments)
                .extracting("name", "type")
                .containsExactly(tuple("attachmentWithoutTitle", null));

    }

    @Test
    void shouldProcessNullAttachment() {
        final AllureResults results = runWithinTestContext(
                this::attachmentWithNullValue,
                Allure1AttachAspects::setLifecycle
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getAttachments)
                .extracting("name", "type")
                .containsExactly(tuple("attachmentWithNullValue", null));
    }

    @SuppressWarnings("all")
    @Attachment
    byte[] attachmentWithNullValue() {
        return null;
    }

    @SuppressWarnings("all")
    @Attachment
    byte[] attachmentWithoutTitle() {
        return new byte[]{};
    }

    @SuppressWarnings({"all"})
    @Attachment(value = "attachment with {0}", type = "text/plain")
    byte[] attachmentWithTitleAndType(String parameter) {
        return new byte[]{};
    }

}
