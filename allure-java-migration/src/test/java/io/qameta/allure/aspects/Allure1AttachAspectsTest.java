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
