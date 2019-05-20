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

import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static org.openqa.selenium.logging.LogType.BROWSER;
import static org.openqa.selenium.logging.LogType.CLIENT;
import static org.openqa.selenium.logging.LogType.DRIVER;
import static org.openqa.selenium.logging.LogType.PERFORMANCE;
import static org.openqa.selenium.logging.LogType.PROFILER;
import static org.openqa.selenium.logging.LogType.SERVER;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Artem Eroshenko.
 */
@SuppressWarnings("unused")
public class AllureSelenide implements LogEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureSelenide.class);

    private boolean saveScreenshots = true;
    private boolean savePageHtml = true;
    private final List<String> supportedLogTypes = Arrays.asList(BROWSER, CLIENT, DRIVER, PERFORMANCE, PROFILER, SERVER);
    private final List<String> logTypesToSave = new ArrayList<>();

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

    public AllureSelenide saveLogsFrom(String logType) {
        if (supportedLogTypes.contains(logType)) {
            logTypesToSave.add(logType);
        } else {
            LOGGER.error("Unsupported log type: {}", logType);
        }
        return this;
    }

    private static Optional<byte[]> getScreenshotBytes() {
        try {
            return Optional.of((TakesScreenshot) WebDriverRunner.getWebDriver())
                    .map(wd -> wd.getScreenshotAs(OutputType.BYTES));
        } catch (WebDriverException e) {
            LOGGER.warn("Could not get screen shot", e);
            return Optional.empty();
        }
    }

    private static Optional<byte[]> getPageSourceBytes() {
        try {
            return Optional.of(WebDriverRunner.getWebDriver())
                    .map(WebDriver::getPageSource)
                    .map(ps -> ps.getBytes(StandardCharsets.UTF_8));
        } catch (WebDriverException e) {
            LOGGER.warn("Could not get page source", e);
            return Optional.empty();
        }
    }

    private static String getBrowserLogs(String logType) {
        return String.join("\n\n", Selenide.getWebDriverLogs(logType, Level.ALL));
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
                                .forEach(logType -> {
                                    final byte[] content = getBrowserLogs(logType).getBytes(StandardCharsets.UTF_8);
                                    lifecycle.addAttachment("Logs from: " + logType, "text/plain", ".txt", content);
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
