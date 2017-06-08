package io.qameta.allure.aspects;

import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.aspects.testdata.AllureResultsWriterStub;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Attachment;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * eroshenkoam
 * 30.04.17
 */
public class Allure1AttachAspectsTest {

    private AllureResultsWriterStub results;

    private AllureLifecycle lifecycle;

    @Before
    public void initLifecycle() {
        results = new AllureResultsWriterStub();
        lifecycle = new AllureLifecycle(results);
        Allure1AttachAspects.setLifecycle(lifecycle);
    }

    @Test
    public void shouldSetupAttachmentTitleFromAnnotation() {
        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().withUuid(uuid);

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);

        attachmentWithTitleAndType("parameter value");

        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getAttachments)
                .extracting("name", "type")
                .containsExactly(tuple("attachment with parameter value", "text/plain"));

    }

    @Test
    public void shouldSetupAttachmentTitleFromMethodSignature() {
        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().withUuid(uuid);

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);

        attachmentWithoutTitle();

        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getAttachments)
                .extracting("name", "type")
                .containsExactly(tuple("attachmentWithoutTitle", null));

    }

    @Test
    public void shouldProcessNullAttachment() throws Exception {
        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().withUuid(uuid);

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);

        attachmentWithNullValue();

        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getAttachments)
                .extracting("name", "type")
                .containsExactly(tuple("attachmentWithNullValue", null));
    }

    @Attachment
    public byte[] attachmentWithNullValue() {
        return null;
    }

    @Attachment
    public byte[] attachmentWithoutTitle() {
        return new byte[]{};
    }

    @Attachment(value = "attachment with {0}", type = "text/plain")
    public byte[] attachmentWithTitleAndType(String parameter) {
        return new byte[]{};
    }
}
