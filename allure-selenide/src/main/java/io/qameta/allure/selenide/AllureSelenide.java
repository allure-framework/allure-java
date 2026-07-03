/*
 *  Copyright 2016-2026 Qameta Software Inc
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
import com.codeborne.selenide.logevents.SelenideLog;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.AttachmentOptions;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Integrates Selenide with Allure reporting.
 *
 * <p>Register this type through the standard Selenide extension, listener, interceptor, or plugin mechanism so framework execution events are written to Allure results. Use explicit dependencies when embedding the integration in tests or custom runtimes.</p>
 */
@SuppressWarnings("unused")
public class AllureSelenide implements LogEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureSelenide.class);

    private boolean saveScreenshots = true;
    private boolean savePageHtml = true;
    private boolean includeSelenideLocatorsSteps = true;
    private final Map<LogType, Level> logTypesToSave = new EnumMap<>(LogType.class);
    private final AllureLifecycle lifecycle;

    /**
     * Creates an Allure selenide with default configuration.
     */
    public AllureSelenide() {
        this(Allure.getLifecycle());
    }

    /**
     * Creates an Allure selenide with the supplied values.
     *
     * @param lifecycle the Allure lifecycle to use
     */
    public AllureSelenide(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    /**
     * Configures screenshots.
     *
     * @param saveScreenshots the save screenshots
     * @return this instance for method chaining
     */
    public AllureSelenide screenshots(final boolean saveScreenshots) {
        this.saveScreenshots = saveScreenshots;
        return this;
    }

    /**
     * Configures save page source.
     *
     * @param savePageHtml the save page html
     * @return this instance for method chaining
     */
    public AllureSelenide savePageSource(final boolean savePageHtml) {
        this.savePageHtml = savePageHtml;
        return this;
    }

    /**
     * Configures include selenide steps.
     *
     * @param includeSelenideSteps the include selenide steps
     * @return this instance for method chaining
     */
    public AllureSelenide includeSelenideSteps(final boolean includeSelenideSteps) {
        this.includeSelenideLocatorsSteps = includeSelenideSteps;
        return this;
    }

    /**
     * Configures enable logs.
     *
     * @param logType the log type
     * @param logLevel the log level
     * @return this instance for method chaining
     */
    public AllureSelenide enableLogs(final LogType logType, final Level logLevel) {
        logTypesToSave.put(logType, logLevel);

        return this;
    }

    /**
     * Configures disable logs.
     *
     * @param logType the log type
     * @return this instance for method chaining
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeEvent(final LogEvent event) {
        if (stepsShouldBeLogged(event)) {
            lifecycle.getCurrentExecutableKey().ifPresent(
                    parent -> lifecycle.startStep(new StepResult().setName(event.toString()))
            );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterEvent(final LogEvent event) {
        if (event.getStatus().equals(LogEvent.EventStatus.FAIL)) {
            lifecycle.getCurrentExecutableKey().ifPresent(owner -> {
                if (saveScreenshots) {
                    getScreenshotBytes()
                            .ifPresent(
                                    bytes -> lifecycle.addAttachment(
                                            owner,
                                            "Screenshot",
                                            "image/png",
                                            new ByteArrayInputStream(bytes),
                                            AttachmentOptions.empty()
                                    )
                            );
                }
                if (savePageHtml) {
                    getPageSourceBytes()
                            .ifPresent(
                                    bytes -> lifecycle.addAttachment(
                                            owner,
                                            "Page source",
                                            "text/html",
                                            new ByteArrayInputStream(bytes),
                                            AttachmentOptions.empty()
                                    )
                            );
                }
                if (!logTypesToSave.isEmpty()) {
                    logTypesToSave
                            .forEach((logType, level) -> {
                                final byte[] content = getBrowserLogs(logType, level).getBytes(UTF_8);
                                lifecycle.addAttachment(
                                        owner,
                                        "Logs from: " + logType,
                                        "application/json",
                                        new ByteArrayInputStream(content),
                                        AttachmentOptions.withFileExtension(".txt")
                                );
                            });
                }
            });
        }

        if (stepsShouldBeLogged(event)) {
            lifecycle.getCurrentExecutableKey().ifPresent(owner -> {
                switch (event.getStatus()) {
                    case PASS:
                        lifecycle.updateStep(step -> step.setStatus(Status.PASSED));
                        break;
                    case FAIL:
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

    private boolean stepsShouldBeLogged(final LogEvent event) {
        //  other customer Loggers could be configured, they should be logged
        return includeSelenideLocatorsSteps || !(event instanceof SelenideLog);
    }
}
