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

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.codeborne.selenide.logevents.LogEvent;
import com.codeborne.selenide.logevents.LogEventListener;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Artem Eroshenko.
 */
@SuppressWarnings("unused")
public class AllureSelenide implements LogEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureSelenide.class);

    private boolean saveScreenshots = true;
    private boolean savePageHtml = true;
    private final Map<LogType, Level> logTypesToSave = new HashMap<>();
    private final AllureLifecycle lifecycle;

    public AllureSelenide() {
        this(Allure.getLifecycle());
    }

    public AllureSelenide(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public AllureSelenide screenshots(final boolean saveScreenshots) {
        this.saveScreenshots = saveScreenshots;
        return this;
    }

    public AllureSelenide savePageSource(final boolean savePageHtml) {
        this.savePageHtml = savePageHtml;
        return this;
    }

    public AllureSelenide enableLogs(final LogType logType, final Level logLevel) {
        logTypesToSave.put(logType, logLevel);

        return this;
    }

    public AllureSelenide disableLogs(final LogType logType) {
        logTypesToSave.remove(logType);

        return this;
    }

    private static Optional<byte[]> getScreenshotBytes() {
        try {
            return WebDriverRunner.hasWebDriverStarted()
                ? Optional.of(((TakesScreenshot) WebDriverRunner.getWebDriver()).getScreenshotAs(OutputType.BYTES))
                : Optional.empty();
        } catch (WebDriverException e) {
            LOGGER.warn("Could not get screen shot", e);
            return Optional.empty();
        }
    }

    private static Optional<byte[]> getPageSourceBytes() {
        try {
            return WebDriverRunner.hasWebDriverStarted()
                ? Optional.of(WebDriverRunner.getWebDriver().getPageSource().getBytes(UTF_8))
                : Optional.empty();
        } catch (WebDriverException e) {
            LOGGER.warn("Could not get page source", e);
            return Optional.empty();
        }
    }

    private static String getBrowserLogs(final LogType logType, final Level level) {
        return String.join("\n\n", Selenide.getWebDriverLogs(logType.toString(), level));
    }

    @Override
    public void beforeEvent(final LogEvent event) {
        lifecycle.getCurrentTestCaseOrStep().ifPresent(parentUuid -> {
            final String uuid = UUID.randomUUID().toString();
            lifecycle.startStep(parentUuid, uuid, new StepResult().setName(event.toString()));
        });
    }

    @Override
    public void afterEvent(final LogEvent event) {
        lifecycle.getCurrentTestCaseOrStep().ifPresent(parentUuid -> {
            switch (event.getStatus()) {
                case PASS:
                    lifecycle.updateStep(step -> step.setStatus(Status.PASSED));
                    break;
                case FAIL:
                    if (saveScreenshots) {
                        getScreenshotBytes()
                                .ifPresent(bytes -> lifecycle.addAttachment("Screenshot", "image/png", "png", bytes));
                    }
                    if (savePageHtml) {
                        getPageSourceBytes()
                                .ifPresent(bytes -> lifecycle.addAttachment("Page source", "text/html", "html", bytes));
                    }
                    if (!logTypesToSave.isEmpty()) {
                        logTypesToSave
                            .forEach((logType, level) -> {
                                final byte[] content = getBrowserLogs(logType, level).getBytes(UTF_8);
                                lifecycle.addAttachment("Logs from: " + logType, "application/json", ".txt", content);
                                });
                    }
                    lifecycle.updateStep(stepResult -> {
                        stepResult.setStatus(getStatus(event.getError()).orElse(Status.BROKEN));
                        stepResult.setStatusDetails(getStatusDetails(event.getError()).orElse(new StatusDetails()));
                    });
                    break;
                default:
                    LOGGER.warn("Step finished with unsupported status {}", event.getStatus());
                    break;
            }
            lifecycle.stopStep();
        });
    }
}
