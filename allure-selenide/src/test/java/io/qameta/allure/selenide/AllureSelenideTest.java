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
package io.qameta.allure.selenide;

import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.logevents.LogEvent;
import com.codeborne.selenide.logevents.SelenideLog;
import com.codeborne.selenide.logevents.SelenideLogger;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.test.AllureFeatures;
import io.qameta.allure.test.AllureResults;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.chrome.ChromeDriver;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author charlie (Dmitry Baev).
 */
class AllureSelenideTest {

    @AllureFeatures.Steps
    @Test
    void shouldLogPassedSteps() {
        final AllureResults results = runWithinTestContext(() -> {
            final AllureSelenide selenide = new AllureSelenide()
                    .savePageSource(false)
                    .screenshots(false);
            SelenideLogger.addListener(UUID.randomUUID().toString(), selenide);
            final SelenideLog log = SelenideLogger.beginStep(
                    "dummy source",
                    "dummyMethod()",
                    "param1",
                    "param2"
            );
            SelenideLogger.commitStep(log, LogEvent.EventStatus.PASS);
        });

        final StepResult selenideStep = extractStepFromResults(results);
        assertThat(selenideStep)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(
                        "$(dummy source) dummy method()([param1, param2])",
                        Status.PASSED
                );
    }

    @AllureFeatures.Steps
    @Test
    void shouldLogStepTimings() {
        final AllureResults results = runWithinTestContext(() -> {
            final AllureSelenide selenide = new AllureSelenide()
                    .savePageSource(false)
                    .screenshots(false);
            SelenideLogger.addListener(UUID.randomUUID().toString(), selenide);
            final SelenideLog log = SelenideLogger.beginStep(
                    "dummy source",
                    "dummyMethod()",
                    "param1",
                    "param2"
            );
            SelenideLogger.commitStep(log, LogEvent.EventStatus.PASS);
        });

        final StepResult selenideStep = extractStepFromResults(results);
        assertThat(selenideStep)
                .as("start timestamp")
                .extracting(StepResult::getStart)
                .isNotNull();

        assertThat(selenideStep)
                .as("stop timestamp")
                .extracting(StepResult::getStop)
                .isNotNull();
    }

    @AllureFeatures.Attachments
    @Test
    void shouldSaveScreenshotsOnFail() {
        final ChromeDriver wdMock = mock(ChromeDriver.class);
        WebDriverRunner.setWebDriver(wdMock);
        doReturn("hello".getBytes(StandardCharsets.UTF_8))
                .when(wdMock).getScreenshotAs(OutputType.BYTES);

        final AllureResults results = runWithinTestContext(() -> {
            final AllureSelenide selenide = new AllureSelenide()
                    .savePageSource(false)
                    .screenshots(true);
            SelenideLogger.addListener(UUID.randomUUID().toString(), selenide);
            final SelenideLog log = SelenideLogger.beginStep(
                    "dummy source",
                    "dummyMethod()",
                    "param1",
                    "param2"
            );
            SelenideLogger.commitStep(log, new Exception("something went wrong"));
        });

        final StepResult selenideStep = extractStepFromResults(results);
        assertThat(selenideStep.getAttachments())
                .hasSize(1);

        final Attachment attachment = selenideStep.getAttachments().iterator().next();
        assertThat(results.getAttachments())
                .containsKey(attachment.getSource());

        final String attachmentContent = new String(
                results.getAttachments().get(attachment.getSource()),
                StandardCharsets.UTF_8
        );

        assertThat(attachmentContent)
                .isEqualTo("hello");
    }

    @AllureFeatures.Attachments
    @Test
    void shouldSavePageSourceOnFail() {
        final ChromeDriver wdMock = mock(ChromeDriver.class);
        WebDriverRunner.setWebDriver(wdMock);
        doReturn("dummy-page-source")
                .when(wdMock).getPageSource();

        final AllureResults results = runWithinTestContext(() -> {
            final AllureSelenide selenide = new AllureSelenide()
                    .screenshots(false)
                    .savePageSource(true);
            SelenideLogger.addListener(UUID.randomUUID().toString(), selenide);
            final SelenideLog log = SelenideLogger.beginStep(
                    "dummy source",
                    "dummyMethod()",
                    "param1",
                    "param2"
            );
            SelenideLogger.commitStep(log, new Exception("something went wrong"));
        });

        final StepResult selenideStep = extractStepFromResults(results);
        assertThat(selenideStep.getAttachments())
                .hasSize(1);

        final Attachment attachment = selenideStep.getAttachments().iterator().next();
        assertThat(results.getAttachments())
                .containsKey(attachment.getSource());

        final String attachmentContent = new String(
                results.getAttachments().get(attachment.getSource()),
                StandardCharsets.UTF_8
        );

        assertThat(attachmentContent)
                .isEqualTo("dummy-page-source");
    }

    @AllureFeatures.Steps
    @Test
    void shouldLogFailedSteps() {
        final AllureResults results = runWithinTestContext(() -> {
            final AllureSelenide selenide = new AllureSelenide()
                    .savePageSource(false)
                    .screenshots(false);
            SelenideLogger.addListener(UUID.randomUUID().toString(), selenide);
            final SelenideLog log = SelenideLogger.beginStep(
                    "dummy source",
                    "dummyMethod()",
                    "param1",
                    "param2"
            );
            SelenideLogger.commitStep(log, new Exception("something went wrong"));
        });

        final StepResult selenideStep = extractStepFromResults(results);
        assertThat(selenideStep)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(
                        "$(dummy source) dummy method()([param1, param2])",
                        Status.BROKEN
                );

        assertThat(selenideStep)
                .extracting(s -> s.getStatusDetails().getMessage())
                .isEqualTo("something went wrong");
    }

    @AllureFeatures.Steps
    @Test
    void shouldSupportNestedSteps() {
        final AllureResults results = runWithinTestContext(() -> {
            final AllureSelenide selenide = new AllureSelenide()
                    .savePageSource(false)
                    .screenshots(false);
            SelenideLogger.addListener(UUID.randomUUID().toString(), selenide);
            final SelenideLog log = SelenideLogger.beginStep(
                    "dummy source",
                    "dummyMethod()",
                    "param1",
                    "param2"
            );

            Allure.step("child1");
            Allure.step("child2");
            Allure.step("child3");

            SelenideLogger.commitStep(log, LogEvent.EventStatus.PASS);
        });

        final StepResult selenideStep = extractStepFromResults(results);
        assertThat(selenideStep.getSteps())
                .extracting(StepResult::getName)
                .containsExactly("child1", "child2", "child3");
    }

    private static StepResult extractStepFromResults(AllureResults results) {
        return results
                .getTestResults().iterator().next()
                .getSteps().iterator().next();
    }
}
