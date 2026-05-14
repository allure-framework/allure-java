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
package io.qameta.allure.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Description;
import io.qameta.allure.Param;
import io.qameta.allure.Step;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureFeatures;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.AllureResultsWriterStub;
import io.qameta.allure.test.AllureTestCommonsUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static io.qameta.allure.model.Parameter.Mode.HIDDEN;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

@ResourceLock(
        value = "io.qameta.allure.playwright.lifecycle",
        mode = READ_WRITE
)
class AllurePlaywrightTest {

    private static final byte[] PNG_HEADER = new byte[]{(byte) 0x89, 'P', 'N', 'G'};
    private static final byte[] ZIP_HEADER = new byte[]{'P', 'K'};
    private static final String FAILURE_HTML = "<html><body><h1>Failure diagnostics</h1></body></html>";

    private static Playwright playwright;
    private static Browser browser;

    private final List<BrowserContext> contexts = new ArrayList<>();

    @BeforeAll
    static void startBrowser() {
        try {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(true)
                            .setArgs(Collections.singletonList("--no-sandbox"))
            );
        } catch (PlaywrightException e) {
            closeBrowser();
            assumeTrue(false, "Chromium is not available for Playwright integration tests: " + e.getMessage());
        }
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            close(browser::close);
        }
        if (playwright != null) {
            close(playwright::close);
        }
        browser = null;
        playwright = null;
    }

    @AfterEach
    void clearRegistry() {
        for (BrowserContext context : contexts) {
            close(context::close);
        }
        contexts.clear();
        AllurePlaywright.clear();
    }

    /**
     * Checks that a real Playwright browser action is reported as an Allure step.
     *
     * The test clicks a button on a Chromium page and verifies that the aspect records one passed action step with the
     * expected human-readable name.
     */
    @Description(useJavaDoc = true)
    @AllureFeatures.Steps
    @Test
    void shouldLogPlaywrightActionSteps() {
        final Page page = newPageWithContent("<button>Save</button>");

        final AllureResults results = runWithPlaywrightContext(() -> page.click("button"));

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(tuple("Click button", Status.PASSED));
    }

    /**
     * Checks that Playwright action steps are nested under user-created Allure runtime steps.
     *
     * The test wraps a real Playwright click in {@code Allure.step(...)} and verifies that Allure keeps the user step as
     * the parent while reporting the Playwright action as its child step.
     */
    @Description(useJavaDoc = true)
    @AllureFeatures.Steps
    @Test
    void shouldNestPlaywrightActionStepsInsideRuntimeSteps() {
        final Page page = newPageWithContent("<button>Pay</button>");

        final AllureResults results = runWithPlaywrightContext(
                () -> Allure.step("Checkout flow", () -> page.click("button"))
        );

        final StepResult step = results.getTestResults().iterator().next().getSteps().iterator().next();
        assertThat(step)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly("Checkout flow", Status.PASSED);
        assertThat(step.getSteps())
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly(tuple("Click button", Status.PASSED));
    }

    /**
     * Checks that sensitive action parameters are redacted in generated step names.
     *
     * The test fills an input with a real value and verifies that the reported Playwright step contains the selector but
     * replaces the typed value with the configured redaction marker.
     */
    @Description(useJavaDoc = true)
    @AllureFeatures.Steps
    @Test
    void shouldRedactTypedValuesInActionStepNames() {
        final Page page = newPageWithContent("<input id='password'>");

        final AllureResults results = runWithPlaywrightContext(() -> page.fill("#password", "secret"));

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("Fill #password with <redacted>");
    }

    /**
     * Checks that Playwright action failures keep their original exception details in Allure.
     *
     * The test clicks a missing element with a short timeout and verifies that the reported action step is marked broken
     * and includes the Playwright failure message.
     */
    @Description(useJavaDoc = true)
    @AllureFeatures.Steps
    @Test
    void shouldPreservePlaywrightActionFailures() {
        final Page page = newPageWithContent("<button>Present</button>");

        final AllureResults results = runWithPlaywrightContext(
                () -> page.click("#missing", new Page.ClickOptions().setTimeout(100))
        );

        final StepResult step = results.getTestResults().iterator().next().getSteps().iterator().next();
        assertThat(step)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly("Click #missing", Status.BROKEN);
        assertThat(message(step.getStatusDetails()))
                .contains("#missing");
    }

    /**
     * Checks that user-created Playwright screenshots are attached automatically.
     *
     * The test calls the real {@code Page.screenshot(...)} API and verifies that the returned bytes are attached to the
     * screenshot step without changing the value returned to the test.
     */
    @Description(useJavaDoc = true)
    @AllureFeatures.Attachments
    @Test
    void shouldAttachUserScreenshotsAutomatically() {
        final AtomicReference<byte[]> screenshot = new AtomicReference<>();
        final Page page = newPageWithContent("<main style='width: 100px; height: 40px'>Screenshot</main>");

        final AllureResults results = runWithPlaywrightContext(
                () -> screenshot.set(page.screenshot(new Page.ScreenshotOptions()))
        );

        final StepResult step = results.getTestResults().iterator().next().getSteps().iterator().next();
        final Attachment attachment = step.getAttachments().iterator().next();

        assertThat(step)
                .extracting(StepResult::getName, StepResult::getStatus)
                .containsExactly("Take screenshot", Status.PASSED);
        assertThat(attachment)
                .extracting(Attachment::getName, Attachment::getType)
                .containsExactly("Screenshot", "image/png");
        assertAttachmentContent(results, screenshot.get());
    }

    /**
     * Checks that the public screenshot helper attaches a page image without creating an extra action step.
     *
     * The helper suppresses the Playwright aspect while it captures the screenshot, so the test verifies that only the
     * attachment is written and the generated PNG bytes are present in Allure results.
     */
    @Description(useJavaDoc = true)
    @AllureFeatures.Attachments
    @Test
    void shouldAttachScreenshotThroughHelperWithoutDuplicateAspectAttachment() {
        final Page page = newPageWithContent("<main style='width: 100px; height: 40px'>Current page</main>");

        final AllureResults results = runWithPlaywrightContext(
                () -> AllurePlaywright.attachScreenshot("Current page", page)
        );

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .isEmpty();
        assertThat(results.getAttachments())
                .hasSize(1);
        assertAttachmentStartsWith(results, PNG_HEADER);
    }

    /**
     * Checks that explicitly registered pages produce failure diagnostics.
     *
     * The test registers a real page, invokes the failure hook, and verifies that Allure receives both a screenshot and
     * the current page source for the failed test.
     */
    @Description(useJavaDoc = true)
    @AllureFeatures.Attachments
    @Test
    void shouldAttachFailureDiagnosticsForRegisteredPages() {
        final Page page = newPageWithContent(FAILURE_HTML);

        final AllureResults results = runWithPlaywrightContext(() -> {
            AllurePlaywright.register(page);
            AllurePlaywright.afterTestFailure(new AssertionError("boom"));
        });

        assertFailureDiagnostics(results);
    }

    /**
     * Checks that the Allure test lifecycle listener attaches diagnostics for failed tests.
     *
     * The test marks the current Allure test as failed and verifies that the Playwright lifecycle hook adds screenshot
     * and page source artifacts before the test result is written.
     */
    @Description(useJavaDoc = true)
    @AllureFeatures.Attachments
    @Test
    void shouldAttachFailureDiagnosticsFromAllureLifecycle() {
        final Page page = newPageWithContent(FAILURE_HTML);

        final AllureResults results = runWithPlaywrightContext(() -> {
            AllurePlaywright.register(page);
            Allure.getLifecycle().updateTestCase(testResult -> testResult.setStatus(Status.FAILED));
        });

        assertFailureDiagnostics(results);
    }

    /**
     * Checks that pages touched by reported Playwright actions are registered automatically.
     *
     * The test performs a real click, triggers failure diagnostics, and verifies that the page observed by the aspect is
     * used for screenshot and page source attachments.
     */
    @Description(useJavaDoc = true)
    @AllureFeatures.Attachments
    @Test
    void shouldRegisterPageWhenPlaywrightActionIsReported() {
        final Page page = newPageWithContent("<button>Attach diagnostics</button>" + FAILURE_HTML);

        final AllureResults results = runWithPlaywrightContext(() -> {
            page.click("button");
            AllurePlaywright.afterTestFailure(new AssertionError("boom"));
        });

        assertFailureDiagnostics(results);
    }

    /**
     * Checks that pages created through Playwright contexts are registered automatically.
     *
     * The test creates a page from a real browser context, triggers failure diagnostics, and verifies that the created
     * page contributes screenshot and page source artifacts even without manual registration.
     */
    @Description(useJavaDoc = true)
    @AllureFeatures.Attachments
    @Test
    void shouldRegisterCreatedPagesAutomatically() {
        final AllureResults results = runWithPlaywrightContext(() -> {
            final BrowserContext context = newContext();
            final Page page = context.newPage();
            AllurePlaywright.withAspectSuppressed(() -> {
                page.setContent(FAILURE_HTML);
                return null;
            });
            AllurePlaywright.afterTestFailure(new AssertionError("boom"));
        });

        assertFailureDiagnostics(results);
    }

    /**
     * Checks that a tracing session attaches its trace archive when closed.
     *
     * The test starts Playwright tracing on a real context, performs browser activity, closes the returned session, and
     * verifies that Allure receives a zip trace attachment.
     */
    @Description(useJavaDoc = true)
    @AllureFeatures.Attachments
    @Test
    void shouldAttachTraceWhenSessionIsClosed() {
        final BrowserContext context = newContext();
        final Page page = context.newPage();

        final AllureResults results = runWithPlaywrightContext(() -> {
            final TraceSession traceSession = AllurePlaywright.startTracing("Trace", context);
            page.setContent("<button>Trace</button>");
            page.click("button");
            traceSession.close();
        });

        assertThat(results.getAttachments())
                .hasSize(1);
        assertAttachmentStartsWith(results, ZIP_HEADER);
    }

    /**
     * Checks that registered tracing is attached when a browser context closes.
     *
     * The test starts tracing, closes the real Playwright context, and verifies that the close hook stops tracing and
     * writes the generated trace archive to Allure.
     */
    @Description(useJavaDoc = true)
    @AllureFeatures.Attachments
    @Test
    void shouldAttachTraceWhenContextIsClosed() {
        final BrowserContext context = newContext();
        final Page page = context.newPage();

        final AllureResults results = runWithPlaywrightContext(() -> {
            AllurePlaywright.startTracing("Trace", context);
            page.setContent("<button>Trace</button>");
            context.close();
        });

        assertThat(results.getAttachments())
                .hasSize(1);
        assertAttachmentStartsWith(results, ZIP_HEADER);
    }

    /**
     * Checks that unfinished tracing sessions are attached during Allure test shutdown.
     *
     * The test leaves an active Playwright trace open and verifies that the Allure lifecycle hook stops it and attaches
     * the generated trace archive before clearing per-test state.
     */
    @Description(useJavaDoc = true)
    @AllureFeatures.Attachments
    @Test
    void shouldAttachTraceWhenAllureLifecycleStopsTest() {
        final BrowserContext context = newContext();
        final Page page = context.newPage();

        final AllureResults results = runWithPlaywrightContext(() -> {
            AllurePlaywright.startTracing("Trace", context);
            page.setContent("<button>Trace</button>");
        });

        assertThat(results.getAttachments())
                .hasSize(1);
        assertAttachmentStartsWith(results, ZIP_HEADER);
    }

    /**
     * Checks that page console messages and page errors are attached when a context closes.
     *
     * The test emits a browser console error and an uncaught page error, closes the real context, and verifies that both
     * diagnostic text attachments are present.
     */
    @Description(useJavaDoc = true)
    @AllureFeatures.Attachments
    @Test
    void shouldAttachPageLogsWhenContextIsClosed() {
        final BrowserContext context = newContext();
        final Page page = context.newPage();

        final AllureResults results = runWithPlaywrightContext(() -> {
            page.setContent(
                    "<script>"
                            + "console.error('console boom');"
                            + "setTimeout(() => { throw new Error('page boom'); }, 0);"
                            + "</script>"
            );
            page.waitForTimeout(250);
            context.close();
        });

        assertThat(results.getAttachments())
                .hasSize(2);
        assertTextAttachmentContent(results, "console boom");
        assertTextAttachmentContent(results, "page boom");
    }

    /**
     * Checks that Playwright-recorded videos are attached when a video context closes.
     *
     * The test enables video recording on a real context, closes it after page activity, and verifies that exactly one
     * non-empty video attachment is written.
     */
    @Description(useJavaDoc = true)
    @AllureFeatures.Attachments
    @Test
    void shouldAttachVideoWhenContextIsClosed(@TempDir final Path videoDir) {
        final BrowserContext context = newVideoContext(videoDir);
        final Page page = context.newPage();

        final AllureResults results = runWithPlaywrightContext(() -> {
            page.setContent("<h1>Video</h1>");
            page.waitForTimeout(100);
            context.close();
        });

        assertThat(results.getAttachments())
                .hasSize(1);
        assertThat(results.getAttachments().values())
                .anySatisfy(actual -> assertThat(actual).isNotEmpty());
    }

    /**
     * Checks that the public video helper attaches an existing video file.
     *
     * The test writes a temporary WebM file, passes it to {@code AllurePlaywright.attachVideo(...)}, and verifies that
     * the exact file bytes are stored as an Allure attachment.
     */
    @Description(useJavaDoc = true)
    @AllureFeatures.Attachments
    @Test
    void shouldAttachVideoFile(@TempDir final Path tempDir) throws IOException {
        final byte[] video = "video".getBytes(StandardCharsets.UTF_8);
        final Path path = tempDir.resolve("video.webm");
        Files.write(path, video);

        final AllureResults results = runWithPlaywrightContext(() -> AllurePlaywright.attachVideo("Video", path));

        assertThat(results.getAttachments())
                .hasSize(1);
        assertAttachmentContent(results, video);
    }

    /**
     * Checks that helper attachments are ignored when no Allure test or step is active.
     *
     * The test installs a lifecycle without a running test, calls the video helper, and verifies that no attachment is
     * written outside an Allure context.
     */
    @Description(useJavaDoc = true)
    @AllureFeatures.Attachments
    @Test
    void shouldIgnoreMissingAllureContext(@TempDir final Path tempDir) throws IOException {
        final byte[] video = "video".getBytes(StandardCharsets.UTF_8);
        final Path path = tempDir.resolve("video.webm");
        Files.write(path, video);

        final AllureResultsWriterStub writer = runWithoutAllureContext(
                () -> AllurePlaywright.attachVideo("Video", path)
        );

        assertThat(writer.getAttachments())
                .isEmpty();
    }

    @Step("Create Playwright page with HTML content")
    private Page newPageWithContent(@Param(mode = HIDDEN) final String html) {
        final Page page = newContext().newPage();
        page.setContent(html);
        return page;
    }

    @Step("Create Playwright browser context")
    private BrowserContext newContext() {
        final BrowserContext context = browser.newContext();
        contexts.add(context);
        return context;
    }

    @Step("Create Playwright browser context with video recording")
    private BrowserContext newVideoContext(final Path videoDir) {
        final BrowserContext context = browser.newContext(new Browser.NewContextOptions().setRecordVideoDir(videoDir));
        contexts.add(context);
        return context;
    }

    @Step("Close Playwright resource")
    private static void close(@Param(mode = HIDDEN) final Runnable close) {
        try {
            AllurePlaywright.withAspectSuppressed(() -> {
                close.run();
                return null;
            });
        } catch (RuntimeException ignored) {
            // The test may have already closed this Playwright resource.
        }
    }

    @Step("Run Playwright adapter scenario")
    private static AllureResults runWithPlaywrightContext(@Param(mode = HIDDEN) final Runnable runnable) {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        final AllureLifecycle lifecycle = new AllureLifecycle(writer);
        final AllureLifecycle defaultLifecycle = Allure.getLifecycle();
        final AllureLifecycle defaultAspectLifecycle = AllurePlaywrightAspect.getLifecycle();
        try {
            Allure.setLifecycle(lifecycle);
            AllurePlaywrightAspect.setLifecycle(lifecycle);
            runSyntheticTest(runnable, lifecycle);
            return writer;
        } finally {
            Allure.setLifecycle(defaultLifecycle);
            AllurePlaywrightAspect.setLifecycle(defaultAspectLifecycle);
            attachResults(writer);
        }
    }

    @Step("Run helper without active Allure context")
    private static AllureResultsWriterStub runWithoutAllureContext(@Param(mode = HIDDEN) final Runnable runnable) {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        final AllureLifecycle lifecycle = new AllureLifecycle(writer);
        final AllureLifecycle defaultLifecycle = Allure.getLifecycle();
        final AllureLifecycle defaultAspectLifecycle = AllurePlaywrightAspect.getLifecycle();
        try {
            Allure.setLifecycle(lifecycle);
            AllurePlaywrightAspect.setLifecycle(lifecycle);
            runnable.run();
            return writer;
        } finally {
            Allure.setLifecycle(defaultLifecycle);
            AllurePlaywrightAspect.setLifecycle(defaultAspectLifecycle);
            attachResults(writer);
        }
    }

    private static void runSyntheticTest(final Runnable runnable, final AllureLifecycle lifecycle) {
        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().setUuid(uuid);
        try {
            lifecycle.scheduleTestCase(result);
            lifecycle.startTestCase(uuid);
            runnable.run();
        } catch (Throwable e) {
            lifecycle.updateTestCase(uuid, testResult -> {
                getStatus(e).ifPresent(testResult::setStatus);
                getStatusDetails(e).ifPresent(testResult::setStatusDetails);
            });
        } finally {
            lifecycle.stopTestCase(uuid);
            lifecycle.writeTestCase(uuid);
        }
    }

    @Step("Attach collected Allure results")
    private static void attachResults(@Param(mode = HIDDEN) final AllureResults results) {
        AllureTestCommonsUtils.attach(results);
    }

    @Step("Assert failure diagnostics attachments")
    private static void assertFailureDiagnostics(@Param(mode = HIDDEN) final AllureResults results) {
        assertThat(attachments(results))
                .extracting(Attachment::getName, Attachment::getType)
                .contains(
                        tuple("Screenshot", "image/png"),
                        tuple("Page source", "text/html")
                );
        assertAttachmentStartsWith(results, PNG_HEADER);
        assertTextAttachmentContent(results, "Failure diagnostics");
    }

    private static List<Attachment> attachments(final AllureResults results) {
        final List<Attachment> attachments = new ArrayList<>();
        for (TestResult testResult : results.getTestResults()) {
            attachments.addAll(testResult.getAttachments());
            for (StepResult step : testResult.getSteps()) {
                attachments.addAll(step.getAttachments());
            }
        }
        return attachments;
    }

    @Step("Assert exact attachment payload")
    private static void assertAttachmentContent(@Param(mode = HIDDEN) final AllureResults results,
                                                @Param(mode = HIDDEN) final byte[] expected) {
        assertThat(results.getAttachments().values())
                .anySatisfy(actual -> assertThat(actual).containsExactly(expected));
    }

    @Step("Assert attachment payload prefix")
    private static void assertAttachmentStartsWith(@Param(mode = HIDDEN) final AllureResults results,
                                                   @Param(mode = HIDDEN) final byte[] expected) {
        assertThat(results.getAttachments().values())
                .anySatisfy(actual -> assertThat(actual).startsWith(expected));
    }

    private static String message(final StatusDetails statusDetails) {
        return statusDetails == null ? null : statusDetails.getMessage();
    }

    @Step("Assert text attachment contains {expected}")
    private static void assertTextAttachmentContent(@Param(mode = HIDDEN) final AllureResults results,
                                                    final String expected) {
        assertThat(results.getAttachments().values())
                .map(actual -> new String(actual, StandardCharsets.UTF_8))
                .anySatisfy(actual -> assertThat(actual).contains(expected));
    }
}
