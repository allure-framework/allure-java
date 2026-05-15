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
package io.qameta.allure.seleniumbidi;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Param;
import io.qameta.allure.Step;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureFeatures;
import io.qameta.allure.test.AllureResults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.qameta.allure.model.Parameter.Mode.HIDDEN;
import static io.qameta.allure.test.RunUtils.runWithinTestContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

/**
 * Real browser integration tests for the Selenium WebDriver BiDi adapter.
 */
@ResourceLock(
        value = "io.qameta.allure.seleniumbidi.lifecycle",
        mode = READ_WRITE
)
class AllureWebDriverBiDiTest {

    private static final Json JSON = new Json();
    private static final int SELENIUM_PORT = 4444;
    private static final long SELENIUM_SHARED_MEMORY = 2L * 1024 * 1024 * 1024;
    private static final String IMAGE_PROPERTY = "allure.selenium.bidi.integration.image";
    private static final String SELENIUM_IMAGE_TAG = "4.23.0";
    private static final String LOCALHOST_URL_FORMAT = "http://localhost:%d";
    private static final String DEFAULT_PAGE = "/";
    private static final String LOGS_PAGE = "/logs";
    private static final String NETWORK_PAGE = "/network";
    private static final String PING_PATH = "/ping";
    private static final String HTML_TYPE = "text/html";
    private static final String WAIT_FOR_BIDI_INTERRUPTED = "Interrupted while waiting for BiDi events";
    private static final String DROPPED_KEY = "dropped";
    private static final String ENTRIES_KEY = "entries";
    private static final String CONSOLE_LOG = "bidi-it-console";
    private static final String FETCH_LOG = "bidi-it-fetch-ok";
    private static final String SECOND_CONSOLE_LOG = "bidi-it-second-console";
    private static final String CUSTOM_PAGE = "/custom";
    private static final String API_KEY_HEADER_NAME = "X-Api-Key";
    private static final String CUSTOM_HEADER_NAME = "X-Secret-Token";
    private static final String TRACE_HEADER_NAME = "X-Trace-Id";
    private static final String CUSTOM_SECRET = "custom-integration-secret";
    private static final String VISIBLE_HEADER_VALUE = "visible-integration-value";
    private static final String API_KEY = "integration-secret";
    private static final ThreadLocal<Integer> SERVER_PORT = new ThreadLocal<>();

    /**
     * Checks that console log entries are attached as WebDriver BiDi log data.
     *
     * The test emits two browser console messages and verifies that the log attachment contains exactly those messages
     * without exercising network assertions.
     */
    @Description
    @AllureFeatures.Attachments
    @Test
    void shouldAttachConsoleLogs() throws IOException {
        final AllureResults results = runTestWithSeleniumBidi(
                chromeOptions(),
                listener -> listener.network(false),
                driver -> {
                    driver.get(testServerUrl(LOGS_PAGE));
                    waitUntilPageDone(driver);
                    driver.findElement(By.tagName("body"));
                }
        );

        assertThat(attachmentNames(results))
                .containsExactly(BiDiAttachmentStorage.LOG_ATTACHMENT_NAME);
        final Map<String, Object> payload = attachmentPayload(results, BiDiAttachmentStorage.LOG_ATTACHMENT_NAME);
        assertThat(entries(payload))
                .hasSize(2)
                .extracting(entry -> entry.get("text"))
                .containsExactlyInAnyOrder(CONSOLE_LOG, SECOND_CONSOLE_LOG);
        assertDropped(payload, 0);
    }

    /**
     * Checks that console log metadata is attached as WebDriver BiDi log data.
     *
     * The test emits a browser console message and verifies event metadata such as level, method, source, stack trace,
     * timestamp, and argument data.
     */
    @Description
    @AllureFeatures.Attachments
    @Test
    void shouldAttachConsoleLogMetadata() throws IOException {
        final AllureResults results = runTestWithSeleniumBidi(
                chromeOptions(),
                listener -> listener.network(false),
                driver -> {
                    driver.get(testServerUrl(LOGS_PAGE));
                    waitUntilPageDone(driver);
                    driver.findElement(By.tagName("body"));
                }
        );

        final Map<String, Object> payload = attachmentPayload(results, BiDiAttachmentStorage.LOG_ATTACHMENT_NAME);
        final Map<String, Object> entry = entries(payload).stream()
                .filter(item -> CONSOLE_LOG.equals(item.get("text")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Console log entry not found"));
        assertThat(entry)
                .containsEntry("event", "console")
                .containsEntry("level", "info")
                .containsEntry("method", "log")
                .containsEntry("type", "console");
        assertThat(((Number) entry.get("timestamp")).longValue()).isPositive();
        assertThat(object(entry, "source").get("browsingContextId"))
                .isInstanceOf(String.class)
                .asString()
                .isNotBlank();
        assertThat(objects(object(entry, "stackTrace"), "callFrames"))
                .extracting(frame -> String.valueOf(frame.get("url")))
                .anyMatch(url -> url.contains(LOGS_PAGE));
        final Map<String, Object> argument = objects(entry, "args").get(0);
        assertThat(argument)
                .containsEntry("type", "string")
                .containsEntry("value", CONSOLE_LOG);
    }

    /**
     * Checks that request and response metadata are attached as WebDriver BiDi network data.
     *
     * The test performs a browser fetch and verifies that the network attachment contains request and completed response
     * events for the fetched URL.
     */
    @Description
    @AllureFeatures.Attachments
    @Test
    void shouldAttachNetworkEvents() throws IOException {
        final AllureResults results = runTestWithSeleniumBidi(
                chromeOptions(),
                listener -> listener.logs(false),
                driver -> {
                    driver.get(testServerUrl(NETWORK_PAGE));
                    waitUntilPageDone(driver);
                    driver.findElement(By.tagName("body"));
                }
        );

        assertThat(attachmentNames(results))
                .containsExactly(BiDiAttachmentStorage.NETWORK_ATTACHMENT_NAME);
        final Map<String, Object> payload = attachmentPayload(results, BiDiAttachmentStorage.NETWORK_ATTACHMENT_NAME);
        assertThat(object(networkEntry(payload, "beforeRequestSent", PING_PATH), "request").get("url"))
                .asString()
                .contains(PING_PATH);
        assertThat(object(networkEntry(payload, "responseCompleted", PING_PATH), "response").get("url"))
                .asString()
                .contains(PING_PATH);
        assertDropped(payload, 0);
    }

    /**
     * Checks that request metadata is attached as WebDriver BiDi network data.
     *
     * The test performs a browser fetch and verifies the captured request URL, method, request id, timing object, and
     * browser context fields.
     */
    @Description
    @AllureFeatures.Attachments
    @Test
    void shouldAttachNetworkRequestMetadata() throws IOException {
        final AllureResults results = runTestWithSeleniumBidi(
                chromeOptions(),
                listener -> listener.logs(false),
                driver -> {
                    driver.get(testServerUrl(NETWORK_PAGE));
                    waitUntilPageDone(driver);
                    driver.findElement(By.tagName("body"));
                }
        );

        final Map<String, Object> payload = attachmentPayload(results, BiDiAttachmentStorage.NETWORK_ATTACHMENT_NAME);
        final Map<String, Object> entry = networkEntry(payload, "beforeRequestSent", PING_PATH);
        final Map<String, Object> request = object(entry, "request");
        assertThat(String.valueOf(request.get("url"))).contains(PING_PATH);
        assertThat(request.get("method")).isEqualTo("GET");
        assertThat(request.get("requestId"))
                .isInstanceOf(String.class)
                .asString()
                .isNotBlank();
        assertThat(object(request, "timings"))
                .containsKeys("requestStart", "responseEnd");
        assertThat(entry.get("browsingContextId"))
                .isInstanceOf(String.class)
                .asString()
                .isNotBlank();
        assertThat(((Number) entry.get("timestamp")).longValue()).isPositive();
        assertThat(entry.get("blocked")).isEqualTo(false);
        assertThat(((Number) entry.get("redirectCount")).longValue()).isZero();
        assertThat(objects(entry, "intercepts")).isEmpty();
        assertThat(object(entry, "initiator").get("type")).isEqualTo("script");
    }

    /**
     * Checks that response metadata is attached as WebDriver BiDi network data.
     *
     * The test performs a browser fetch and verifies the captured response URL, status, MIME type, protocol, cache flag,
     * size counters, and headers.
     */
    @Description
    @AllureFeatures.Attachments
    @Test
    void shouldAttachNetworkResponseMetadata() throws IOException {
        final AllureResults results = runTestWithSeleniumBidi(
                chromeOptions(),
                listener -> listener.logs(false),
                driver -> {
                    driver.get(testServerUrl(NETWORK_PAGE));
                    waitUntilPageDone(driver);
                    driver.findElement(By.tagName("body"));
                }
        );

        final Map<String, Object> payload = attachmentPayload(results, BiDiAttachmentStorage.NETWORK_ATTACHMENT_NAME);
        final Map<String, Object> response = object(networkEntry(payload, "responseCompleted", PING_PATH), "response");
        assertThat(String.valueOf(response.get("url"))).contains(PING_PATH);
        assertThat(response)
                .containsEntry("protocol", "http/1.1")
                .containsEntry("statusText", "OK")
                .containsEntry("fromCache", false)
                .containsEntry("mimeType", "text/plain");
        assertThat(((Number) response.get("status")).longValue()).isEqualTo(200L);
        assertThat(((Number) response.get("bytesReceived")).longValue()).isPositive();
        assertThat(((Number) response.get("headersSize")).longValue()).isPositive();
        assertThat(headerValue(response, "Content-type")).isEqualTo("text/plain");
    }

    /**
     * Checks that built-in sensitive headers are redacted in network attachments.
     *
     * The test sends an API key request header and verifies that the attached request metadata contains the header name
     * but not the header value.
     */
    @Description
    @AllureFeatures.Attachments
    @Test
    void shouldRedactDefaultSensitiveHeaders() throws IOException {
        final AllureResults results = runTestWithSeleniumBidi(
                chromeOptions(),
                listener -> listener.logs(false),
                driver -> {
                    driver.get(testServerUrl(NETWORK_PAGE));
                    waitUntilPageDone(driver);
                    driver.findElement(By.tagName("body"));
                }
        );

        final Map<String, Object> payload = attachmentPayload(results, BiDiAttachmentStorage.NETWORK_ATTACHMENT_NAME);
        final Map<String, Object> request = object(networkEntry(payload, "beforeRequestSent", PING_PATH), "request");
        assertThat(headerValue(request, API_KEY_HEADER_NAME)).isEqualTo(HeaderRedactor.REDACTED);
        assertThat(attachmentContent(results, BiDiAttachmentStorage.NETWORK_ATTACHMENT_NAME))
                .doesNotContain(API_KEY);
    }

    /**
     * Checks that user-configured sensitive headers are redacted in network attachments.
     *
     * The test sends one custom secret header and one visible tracing header, then verifies that only the tracing value
     * survives redaction.
     */
    @Description
    @AllureFeatures.Attachments
    @Test
    void shouldRedactCustomHeaders() throws IOException {
        final AllureResults results = runTestWithSeleniumBidi(
                chromeOptions(),
                listener -> listener.logs(false).redactHeaders(CUSTOM_HEADER_NAME),
                driver -> {
                    driver.get(testServerUrl(CUSTOM_PAGE));
                    waitUntilPageDone(driver);
                    driver.findElement(By.tagName("body"));
                }
        );

        final Map<String, Object> payload = attachmentPayload(results, BiDiAttachmentStorage.NETWORK_ATTACHMENT_NAME);
        final Map<String, Object> request = object(networkEntry(payload, "beforeRequestSent", PING_PATH), "request");
        assertThat(headerValue(request, CUSTOM_HEADER_NAME)).isEqualTo(HeaderRedactor.REDACTED);
        assertThat(headerValue(request, TRACE_HEADER_NAME)).isEqualTo(VISIBLE_HEADER_VALUE);
        assertThat(attachmentContent(results, BiDiAttachmentStorage.NETWORK_ATTACHMENT_NAME))
                .doesNotContain(CUSTOM_SECRET);
    }

    /**
     * Checks that disabled log capture does not write a log attachment.
     *
     * The test opens a page that emits console logs while log capture is disabled and verifies that only network data is
     * attached.
     */
    @Description
    @AllureFeatures.Attachments
    @Test
    void shouldDisableLogCapture() throws IOException {
        final AllureResults results = runTestWithSeleniumBidi(
                chromeOptions(),
                listener -> listener.logs(false),
                driver -> {
                    driver.get(testServerUrl(LOGS_PAGE));
                    waitUntilPageDone(driver);
                    driver.findElement(By.tagName("body"));
                }
        );

        assertThat(attachmentNames(results))
                .containsExactly(BiDiAttachmentStorage.NETWORK_ATTACHMENT_NAME);
    }

    /**
     * Checks that disabled network capture does not write a network attachment.
     *
     * The test opens a page that emits both console logs and network activity while network capture is disabled, then
     * verifies that only log data is attached.
     */
    @Description
    @AllureFeatures.Attachments
    @Test
    void shouldDisableNetworkCapture() throws IOException {
        final AllureResults results = runTestWithSeleniumBidi(
                chromeOptions(),
                listener -> listener.network(false),
                driver -> {
                    driver.get(testServerUrl(DEFAULT_PAGE));
                    waitUntilPageDone(driver);
                    driver.findElement(By.tagName("body"));
                }
        );

        assertThat(attachmentNames(results))
                .containsExactly(BiDiAttachmentStorage.LOG_ATTACHMENT_NAME);
        final Map<String, Object> payload = attachmentPayload(results, BiDiAttachmentStorage.LOG_ATTACHMENT_NAME);
        assertThat(entries(payload))
                .extracting(entry -> entry.get("text"))
                .contains(CONSOLE_LOG, SECOND_CONSOLE_LOG, FETCH_LOG);
    }

    /**
     * Checks that the log attachment reports dropped entries when the log cap is reached.
     *
     * The test limits log capture to one entry, emits two console messages, and verifies both the retained entry count
     * and dropped counter.
     */
    @Description
    @AllureFeatures.Attachments
    @Test
    void shouldTrackDroppedLogEntries() throws IOException {
        final AllureResults results = runTestWithSeleniumBidi(
                chromeOptions(),
                listener -> listener.network(false).maxLogEntries(1),
                driver -> {
                    driver.get(testServerUrl(LOGS_PAGE));
                    waitUntilPageDone(driver);
                    driver.findElement(By.tagName("body"));
                }
        );

        final Map<String, Object> payload = attachmentPayload(results, BiDiAttachmentStorage.LOG_ATTACHMENT_NAME);
        assertThat(entries(payload)).hasSize(1);
        assertDropped(payload, 1);
    }

    /**
     * Checks that the network attachment reports dropped events when the network cap is reached.
     *
     * The test limits network capture to one event, performs browser network activity, and verifies that additional
     * events are counted as dropped.
     */
    @Description
    @AllureFeatures.Attachments
    @Test
    void shouldTrackDroppedNetworkEvents() throws IOException {
        final AllureResults results = runTestWithSeleniumBidi(
                chromeOptions(),
                listener -> listener.logs(false).maxNetworkEvents(1),
                driver -> {
                    driver.get(testServerUrl(NETWORK_PAGE));
                    waitUntilPageDone(driver);
                    driver.findElement(By.tagName("body"));
                }
        );

        final Map<String, Object> payload = attachmentPayload(results, BiDiAttachmentStorage.NETWORK_ATTACHMENT_NAME);
        assertThat(entries(payload)).hasSize(1);
        assertThat(((Number) payload.get(DROPPED_KEY)).longValue()).isPositive();
    }

    /**
     * Checks that closing the listener flushes buffered BiDi data.
     *
     * The test closes {@link AllureWebDriverBiDi} before WebDriver quit and verifies that collected log data is still
     * attached.
     */
    @Description
    @AllureFeatures.Attachments
    @Test
    void shouldFlushBufferedEventsWhenListenerCloses() throws IOException {
        final AllureResults results = runTestWithSeleniumBidi(
                chromeOptions(),
                listener -> listener.network(false),
                true,
                driver -> {
                    driver.get(testServerUrl(LOGS_PAGE));
                    waitUntilPageDone(driver);
                    driver.findElement(By.tagName("body"));
                }
        );

        final Map<String, Object> payload = attachmentPayload(results, BiDiAttachmentStorage.LOG_ATTACHMENT_NAME);
        assertThat(entries(payload))
                .extracting(entry -> entry.get("text"))
                .contains(CONSOLE_LOG, SECOND_CONSOLE_LOG);
    }

    /**
     * Checks that a driver session without BiDi support does not attach data.
     *
     * The test starts Chrome without BiDi enabled and verifies that browser activity does not produce BiDi attachments.
     */
    @Description
    @AllureFeatures.Attachments
    @Test
    void shouldNotAttachDataWhenBiDiIsUnavailable() throws IOException {
        final AllureResults results = runTestWithSeleniumBidi(chromeOptionsWithoutBiDi(), driver -> {
            driver.get(testServerUrl(DEFAULT_PAGE));
            waitUntilPageDone(driver);
            driver.findElement(By.tagName("body"));
        });

        assertThat(attachmentNames(results)).isEmpty();
    }

    /**
     * Checks that WebDriver interactions inside a user-created runtime step keep the expected Allure step tree.
     *
     * The test opens a real browser page inside {@code Allure.step(...)} and verifies that the runtime step is preserved
     * as the parent for helper steps executed from the lambda, while BiDi data is still attached to the generated result.
     */
    @Description
    @AllureFeatures.Attachments
    @AllureFeatures.Steps
    @Test
    void shouldNestRuntimeStepAroundWebDriverInteractions() throws IOException {
        final AllureResults results = runTestWithSeleniumBidi(
                chromeOptions(),
                listener -> listener.network(false),
                driver -> Allure.step("Open page in runtime step", () -> {
                    driver.get(testServerUrl(LOGS_PAGE));
                    waitUntilPageDone(driver);
                    driver.findElement(By.tagName("body"));
                })
        );

        final TestResult testResult = singleTestResult(results);
        assertThat(attachments(testResult))
                .extracting(Attachment::getName)
                .contains(BiDiAttachmentStorage.LOG_ATTACHMENT_NAME);

        final StepResult scenarioStep = step(testResult.getSteps(), "Run ready Selenium WebDriver scenario");
        assertThat(scenarioStep.getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsSubsequence(
                        tuple("Open page in runtime step", Status.PASSED),
                        tuple("Wait for asynchronous WebDriver BiDi events", Status.PASSED)
                );

        final StepResult runtimeStep = step(scenarioStep.getSteps(), "Open page in runtime step");
        assertThat(runtimeStep)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly("Open page in runtime step", Status.PASSED);
        assertThat(runtimeStep.getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(tuple("Wait until test page finishes browser activity", Status.PASSED));
    }

    @Step("Start Selenium Testcontainers browser")
    private static GenericContainer<?> startSeleniumContainer(final int seleniumPort) {
        final GenericContainer<?> selenium = seleniumContainer(seleniumPort);
        selenium.start();
        return selenium;
    }

    private static GenericContainer<?> seleniumContainer(final int seleniumPort) {
        final GenericContainer<?> container = new GenericContainer<>(seleniumImage())
                .withExposedPorts(SELENIUM_PORT)
                .withSharedMemorySize(SELENIUM_SHARED_MEMORY)
                .withEnv("SE_NODE_GRID_URL", seleniumEndpoint(seleniumPort))
                .waitingFor(Wait.forHttp("/status").forPort(SELENIUM_PORT).forStatusCode(200));
        container.setPortBindings(Collections.singletonList(String.format("%d:%d", seleniumPort, SELENIUM_PORT)));
        return container;
    }

    private static DockerImageName seleniumImage() {
        return DockerImageName.parse(System.getProperty(IMAGE_PROPERTY, defaultImage()));
    }

    private static String defaultImage() {
        final String architecture = System.getProperty("os.arch", "");
        final String image = architecture.contains("aarch64") || architecture.contains("arm64")
                ? "selenium/standalone-chromium"
                : "selenium/standalone-chrome";
        return String.format("%s:%s", image, SELENIUM_IMAGE_TAG);
    }

    @Step("Start local HTTP server")
    private static HttpServer startServer() throws IOException {
        final HttpServer server = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), 0),
                0
        );
        server.createContext(DEFAULT_PAGE, AllureWebDriverBiDiTest::handleIndex);
        server.createContext(LOGS_PAGE, AllureWebDriverBiDiTest::handleLogs);
        server.createContext(NETWORK_PAGE, AllureWebDriverBiDiTest::handleNetwork);
        server.createContext(CUSTOM_PAGE, AllureWebDriverBiDiTest::handleCustom);
        server.createContext(PING_PATH, AllureWebDriverBiDiTest::handlePing);
        server.start();
        return server;
    }

    private static void handleIndex(final HttpExchange exchange) throws IOException {
        respond(
                exchange,
                HTML_TYPE,
                pageScript(
                        consoleLog(CONSOLE_LOG)
                                + consoleLog(SECOND_CONSOLE_LOG)
                                + fetchPing(header(API_KEY_HEADER_NAME, API_KEY))
                                + ".then(response=>response.text())"
                                + ".then(text=>console.log('bidi-it-fetch-'+text))"
                )
        );
    }

    private static void handleLogs(final HttpExchange exchange) throws IOException {
        respond(
                exchange,
                HTML_TYPE,
                pageScript(
                        consoleLog(CONSOLE_LOG)
                                + consoleLog(SECOND_CONSOLE_LOG)
                                + "Promise.resolve()"
                )
        );
    }

    private static void handleNetwork(final HttpExchange exchange) throws IOException {
        respond(
                exchange,
                HTML_TYPE,
                pageScript(fetchPing(header(API_KEY_HEADER_NAME, API_KEY)))
        );
    }

    private static void handleCustom(final HttpExchange exchange) throws IOException {
        respond(
                exchange,
                HTML_TYPE,
                pageScript(
                        fetchPing(
                                header(CUSTOM_HEADER_NAME, CUSTOM_SECRET)
                                        + "," + header(TRACE_HEADER_NAME, VISIBLE_HEADER_VALUE)
                        )
                )
        );
    }

    private static void handlePing(final HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Set-Cookie", "session=server-secret");
        respond(exchange, "text/plain", "ok");
    }

    private static void respond(final HttpExchange exchange,
                                final String contentType,
                                final String body)
            throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static AllureResults runTestWithSeleniumBidi(final ChromeOptions options,
                                                         final WebDriverScenario scenario)
            throws IOException {
        return runTestWithSeleniumBidi(options, listener -> listener, false, scenario);
    }

    private static AllureResults runTestWithSeleniumBidi(final ChromeOptions options,
                                                         final ListenerConfigurer configurer,
                                                         final WebDriverScenario scenario)
            throws IOException {
        return runTestWithSeleniumBidi(options, configurer, false, scenario);
    }

    @Step("Run Selenium WebDriver BiDi test")
    private static AllureResults runTestWithSeleniumBidi(@Param(mode = HIDDEN) final ChromeOptions options,
                                                         @Param(mode = HIDDEN) final ListenerConfigurer configurer,
                                                         final boolean closeListener,
                                                         @Param(mode = HIDDEN) final WebDriverScenario scenario)
            throws IOException {
        final HttpServer server = startServer();
        try {
            final int serverPort = server.getAddress().getPort();
            SERVER_PORT.set(serverPort);
            Testcontainers.exposeHostPorts(serverPort);

            final int seleniumPort = availableTcpPort();
            try (GenericContainer<?> selenium = startSeleniumContainer(seleniumPort)) {
                final URL remoteUrl = seleniumUrl(seleniumPort);
                return runWithinTestContext(() -> {
                    final AllureWebDriverBiDi listener = configurer.configure(
                            new AllureWebDriverBiDi(Allure.getLifecycle(), new SeleniumBiDiSessionFactory())
                    );
                    runWebDriverScenario(
                            listener,
                            new RemoteWebDriver(remoteUrl, options),
                            closeListener,
                            scenario
                    );
                });
            }
        } finally {
            SERVER_PORT.remove();
            server.stop(0);
        }
    }

    @Step("Run ready Selenium WebDriver scenario")
    private static void runWebDriverScenario(@Param(mode = HIDDEN) final AllureWebDriverBiDi listener,
                                             @Param(mode = HIDDEN) final WebDriver rawDriver,
                                             final boolean closeListener,
                                             @Param(mode = HIDDEN) final WebDriverScenario scenario) {
        WebDriver driver = null;
        boolean closed = false;
        try {
            driver = listener.decorate(rawDriver);
            scenario.run(driver);
            waitForBiDiEvents();
            if (closeListener) {
                listener.close();
                rawDriver.quit();
            } else {
                driver.quit();
            }
            closed = true;
        } finally {
            if (!closed) {
                quit(driver, rawDriver);
            }
        }
    }

    private static ChromeOptions chromeOptions() {
        final ChromeOptions options = new ChromeOptions().enableBiDi();
        addChromeArguments(options);
        return options;
    }

    private static ChromeOptions chromeOptionsWithoutBiDi() {
        final ChromeOptions options = new ChromeOptions();
        addChromeArguments(options);
        return options;
    }

    private static void addChromeArguments(final ChromeOptions options) {
        options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage");
    }

    @Step("Wait until test page finishes browser activity")
    private static void waitUntilPageDone(@Param(mode = HIDDEN) final WebDriver driver) {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(AllureWebDriverBiDiTest::isPageDone);
    }

    @Step("Wait for asynchronous WebDriver BiDi events")
    private static void waitForBiDiEvents() {
        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(WAIT_FOR_BIDI_INTERRUPTED, e);
        }
    }

    private static boolean isPageDone(final WebDriver driver) {
        return Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript("return window.__bidiDone === true;"));
    }

    private static URL seleniumUrl(final int seleniumPort) throws IOException {
        return URI.create(seleniumEndpoint(seleniumPort)).toURL();
    }

    private static String seleniumEndpoint(final int seleniumPort) {
        return String.format(LOCALHOST_URL_FORMAT, seleniumPort);
    }

    private static int availableTcpPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static String testServerUrl(final String path) {
        final Integer port = SERVER_PORT.get();
        if (port == null) {
            throw new IllegalStateException("Test server is not running");
        }
        return containerHostServerUrl(port, path);
    }

    private static String containerHostServerUrl(final int port, final String path) {
        return String.format("http://host.testcontainers.internal:%d%s", port, path);
    }

    private static void quit(final WebDriver driver, final WebDriver rawDriver) {
        if (driver != null) {
            driver.quit();
        } else {
            rawDriver.quit();
        }
    }

    @Step("Get single generated test result")
    private static TestResult singleTestResult(@Param(mode = HIDDEN) final AllureResults results) {
        assertThat(results.getTestResults()).hasSize(1);
        final TestResult result = results.getTestResults().iterator().next();
        assertThat(result.getStatus()).isNull();
        return result;
    }

    private static List<String> attachmentNames(final AllureResults results) {
        final List<String> names = new ArrayList<>();
        attachments(singleTestResult(results)).forEach(attachment -> names.add(attachment.getName()));
        return names;
    }

    private static String attachmentContent(final AllureResults results, final String name) {
        return attachmentContent(results, attachments(singleTestResult(results)), name);
    }

    @Step("Read attachment {name}")
    private static String attachmentContent(@Param(mode = HIDDEN) final AllureResults results,
                                            @Param(mode = HIDDEN) final List<Attachment> attachments,
                                            final String name) {
        final Attachment attachment = attachments.stream()
                .filter(item -> name.equals(item.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Attachment not found: " + name));
        return new String(results.getAttachments().get(attachment.getSource()), StandardCharsets.UTF_8);
    }

    @Step("Read JSON attachment {name}")
    private static Map<String, Object> attachmentPayload(@Param(mode = HIDDEN) final AllureResults results,
                                                         final String name) {
        return JSON.toType(attachmentContent(results, name), Json.MAP_TYPE);
    }

    private static List<Attachment> attachments(final TestResult testResult) {
        final List<Attachment> attachments = new ArrayList<>(testResult.getAttachments());
        testResult.getSteps().forEach(step -> collectAttachments(step, attachments));
        return attachments;
    }

    private static void collectAttachments(final StepResult step, final List<Attachment> attachments) {
        attachments.addAll(step.getAttachments());
        step.getSteps().forEach(child -> collectAttachments(child, attachments));
    }

    private static void assertDropped(final Map<String, Object> payload, final long expected) {
        assertThat(((Number) payload.get(DROPPED_KEY)).longValue()).isEqualTo(expected);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> entries(final Map<String, Object> payload) {
        return objects(payload, ENTRIES_KEY);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(final Map<String, Object> payload, final String key) {
        return (Map<String, Object>) payload.get(key);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> objects(final Map<String, Object> payload, final String key) {
        return (List<Map<String, Object>>) payload.get(key);
    }

    private static Map<String, Object> networkEntry(final Map<String, Object> payload,
                                                    final String event,
                                                    final String path) {
        return entries(payload).stream()
                .filter(entry -> event.equals(entry.get("event")))
                .filter(entry -> networkUrl(entry).contains(path))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Network entry not found: " + event + " " + path));
    }

    private static String networkUrl(final Map<String, Object> entry) {
        final Map<String, Object> response = object(entry, "response");
        if (response != null) {
            return String.valueOf(response.get("url"));
        }
        return String.valueOf(object(entry, "request").get("url"));
    }

    private static String headerValue(final Map<String, Object> owner, final String name) {
        return objects(owner, "headers").stream()
                .filter(header -> name.equalsIgnoreCase(String.valueOf(header.get("name"))))
                .map(header -> object(header, "value"))
                .map(value -> String.valueOf(value.get("value")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Header not found: " + name));
    }

    private static StepResult step(final List<StepResult> steps, final String name) {
        return steps.stream()
                .filter(step -> name.equals(step.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Step not found: " + name));
    }

    private static String consoleLog(final String text) {
        return String.format("console.log('%s');", text);
    }

    private static String fetchPing(final String headers) {
        return String.format("fetch('%s',{headers:{%s}})", PING_PATH, headers);
    }

    private static String header(final String name, final String value) {
        return String.format("'%s':'%s'", name, value);
    }

    private static String pageScript(final String script) {
        return "<!doctype html><html><body>"
                + "<script>"
                + "window.__bidiDone=false;"
                + script
                + ".finally(()=>window.__bidiDone=true);"
                + "</script>"
                + "</body></html>";
    }

    @FunctionalInterface
    private interface ListenerConfigurer {

        AllureWebDriverBiDi configure(AllureWebDriverBiDi listener);
    }

    @FunctionalInterface
    private interface WebDriverScenario {

        void run(WebDriver driver);
    }
}
