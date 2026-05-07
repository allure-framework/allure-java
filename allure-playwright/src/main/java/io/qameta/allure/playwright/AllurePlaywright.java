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
import com.microsoft.playwright.ConsoleMessage;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.Video;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.AttachmentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Utility methods for attaching Playwright Java diagnostics to the current Allure test.
 */
public final class AllurePlaywright {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllurePlaywright.class);

    private static final String SCREENSHOT = "Screenshot";
    private static final String PAGE_SOURCE = "Page source";
    private static final String TRACE = "Playwright trace";
    private static final String VIDEO = "Playwright video";
    private static final String CONSOLE_MESSAGES = "Console messages";
    private static final String PAGE_ERRORS = "Page errors";

    private static final ThreadLocal<Boolean> SUPPRESS_ASPECT = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    private AllurePlaywright() {
        throw new IllegalStateException("Do not instance");
    }

    /**
     * Clears Playwright reporting state before a test starts.
     */
    public static void beforeTest() {
        clear();
    }

    /**
     * Attaches final Playwright artifacts and clears reporting state after a test finishes.
     */
    public static void afterTest() {
        attachRegisteredCloseArtifacts();
        clear();
    }

    /**
     * Attaches failure diagnostics for the current test.
     */
    public static void afterTestFailure() {
        attachFailureArtifacts();
    }

    /**
     * Attaches failure diagnostics for the current test.
     *
     * @param throwable the test failure.
     */
    public static void afterTestFailure(final Throwable throwable) {
        LOGGER.debug("Attaching Playwright failure artifacts", throwable);
        afterTestFailure();
    }

    /**
     * Registers a Playwright page for failure diagnostics produced by reporting lifecycle hooks.
     *
     * @param page the page to register.
     */
    public static void register(final Page page) {
        AllurePlaywrightRegistry.register(page);
    }

    /**
     * Registers a Playwright browser context for failure diagnostics produced by reporting lifecycle hooks.
     *
     * @param context the browser context to register.
     */
    public static void register(final BrowserContext context) {
        AllurePlaywrightRegistry.register(context);
    }

    /**
     * Captures and attaches a page screenshot.
     *
     * @param name the attachment name.
     * @param page the page to capture.
     */
    public static void attachScreenshot(final String name, final Page page) {
        if (page == null || !hasAllureContext()) {
            return;
        }
        try {
            final byte[] screenshot = withAspectSuppressed(new Supplier<byte[]>() {
                @Override
                public byte[] get() {
                    return page.screenshot();
                }
            });
            attachBytes(defaultName(name, SCREENSHOT), AttachmentType.PNG, screenshot);
        } catch (RuntimeException e) {
            LOGGER.warn("Could not capture Playwright screenshot", e);
        }
    }

    /**
     * Captures and attaches page source.
     *
     * @param name the attachment name.
     * @param page the page to capture.
     */
    public static void attachPageSource(final String name, final Page page) {
        if (page == null || !hasAllureContext()) {
            return;
        }
        try {
            final String content = withAspectSuppressed(new Supplier<String>() {
                @Override
                public String get() {
                    return page.content();
                }
            });
            attachBytes(defaultName(name, PAGE_SOURCE), AttachmentType.HTML, content.getBytes(StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            LOGGER.warn("Could not capture Playwright page source", e);
        }
    }

    /**
     * Captures and attaches Playwright console messages retained by the page.
     *
     * @param name the attachment name.
     * @param page the page to capture.
     */
    public static void attachConsoleMessages(final String name, final Page page) {
        if (page == null || !hasAllureContext()) {
            return;
        }
        try {
            final String content = formatConsoleMessages(page.consoleMessages());
            attachText(defaultName(name, CONSOLE_MESSAGES), content);
        } catch (RuntimeException e) {
            LOGGER.warn("Could not capture Playwright console messages", e);
        }
    }

    /**
     * Captures and attaches Playwright page errors retained by the page.
     *
     * @param name the attachment name.
     * @param page the page to capture.
     */
    public static void attachPageErrors(final String name, final Page page) {
        if (page == null || !hasAllureContext()) {
            return;
        }
        try {
            final String content = formatLines(page.pageErrors());
            attachText(defaultName(name, PAGE_ERRORS), content);
        } catch (RuntimeException e) {
            LOGGER.warn("Could not capture Playwright page errors", e);
        }
    }

    /**
     * Attaches a Playwright trace archive.
     *
     * @param name the attachment name.
     * @param traceZip the path to the trace zip file.
     */
    public static void attachTrace(final String name, final Path traceZip) {
        attachPath(defaultName(name, TRACE), AttachmentType.ZIP, traceZip);
    }

    /**
     * Attaches a Playwright video file.
     *
     * @param name the attachment name.
     * @param videoFile the path to the video file.
     */
    public static void attachVideo(final String name, final Path videoFile) {
        attachPath(defaultName(name, VIDEO), videoType(videoFile), videoFile);
    }

    /**
     * Starts Playwright tracing and registers the trace for failure diagnostics.
     *
     * @param context the browser context to trace.
     * @return trace session that stops tracing and attaches the generated archive when closed.
     */
    public static TraceSession startTracing(final BrowserContext context) {
        return startTracing(TRACE, context);
    }

    /**
     * Starts Playwright tracing and registers the trace for failure diagnostics.
     *
     * @param name the attachment name to use when the trace is attached.
     * @param context the browser context to trace.
     * @return trace session that stops tracing and attaches the generated archive when closed.
     */
    public static TraceSession startTracing(final String name, final BrowserContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        final Tracing.StartOptions options = new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true);
        context.tracing().start(options);
        final DefaultTraceSession traceSession = new DefaultTraceSession(context, defaultName(name, TRACE));
        AllurePlaywrightRegistry.register(context);
        AllurePlaywrightRegistry.register(traceSession);
        return traceSession;
    }

    static CloseArtifacts beforeClose(final Object target) {
        final CloseArtifacts closeArtifacts = new CloseArtifacts();
        if (target instanceof Page) {
            collectPageCloseArtifacts((Page) target, closeArtifacts);
        } else if (target instanceof BrowserContext) {
            collectContextCloseArtifacts((BrowserContext) target, closeArtifacts);
        } else if (target instanceof Browser) {
            collectBrowserCloseArtifacts((Browser) target, closeArtifacts);
        }
        return closeArtifacts;
    }

    static void attachScreenshotBytes(final String name, final AttachmentType type, final byte[] bytes) {
        if (!AllurePlaywrightConfig.shouldAttachScreenshots()) {
            return;
        }
        attachBytes(defaultName(name, SCREENSHOT), type, bytes);
    }

    static AttachmentType screenshotType(final Object... args) {
        final Object options = args.length == 0 ? null : args[0];
        if (isJpegType(options)) {
            return AttachmentType.JPEG;
        }
        if (isJpegPath(options)) {
            return AttachmentType.JPEG;
        }
        return AttachmentType.PNG;
    }

    static boolean hasAllureContext() {
        return Allure.getLifecycle().getCurrentTestCaseOrStep().isPresent();
    }

    static boolean isAspectSuppressed() {
        return SUPPRESS_ASPECT.get();
    }

    static <T> T withAspectSuppressed(final Supplier<T> supplier) {
        final Boolean previous = SUPPRESS_ASPECT.get();
        SUPPRESS_ASPECT.set(Boolean.TRUE);
        try {
            return supplier.get();
        } finally {
            SUPPRESS_ASPECT.set(previous);
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    static void clear() {
        for (DefaultTraceSession traceSession : AllurePlaywrightRegistry.getTraceSessions()) {
            traceSession.stopWithoutAttachment();
        }
        AllurePlaywrightRegistry.clear();
    }

    static void attachFailureArtifacts() {
        if (!AllurePlaywrightRegistry.markFailureArtifactsAttached()) {
            return;
        }
        if (AllurePlaywrightConfig.shouldAttachFailureScreenshot()) {
            attachFailureScreenshots();
        }
        if (AllurePlaywrightConfig.shouldAttachFailurePageSource()) {
            attachFailurePageSources();
        }
        attachFailureTraces();
    }

    @SuppressWarnings("PMD.CloseResource")
    static void attachRegisteredCloseArtifacts() {
        if (!hasAllureContext()) {
            return;
        }
        if (AllurePlaywrightConfig.shouldAttachClosePageLogs()) {
            for (Page page : AllurePlaywrightRegistry.getPages()) {
                attachPageCloseLogs(page);
            }
        }
        if (AllurePlaywrightConfig.shouldAttachCloseTrace()) {
            attachFailureTraces();
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    private static void collectBrowserCloseArtifacts(final Browser browser, final CloseArtifacts closeArtifacts) {
        for (BrowserContext context : contexts(browser)) {
            collectContextCloseArtifacts(context, closeArtifacts);
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    private static void collectContextCloseArtifacts(final BrowserContext context,
                                                     final CloseArtifacts closeArtifacts) {
        register(context);
        for (Page page : pages(context)) {
            collectPageCloseArtifacts(page, closeArtifacts);
        }
        if (AllurePlaywrightConfig.shouldAttachCloseTrace()) {
            attachCloseTraces(context);
        }
    }

    private static void collectPageCloseArtifacts(final Page page, final CloseArtifacts closeArtifacts) {
        register(page);
        attachPageCloseLogs(page);
        if (AllurePlaywrightRegistry.markCloseVideoAttached(page)) {
            closeArtifacts.addVideo(video(page));
        }
    }

    private static void attachPageCloseLogs(final Page page) {
        if (AllurePlaywrightConfig.shouldAttachClosePageLogs()
                && AllurePlaywrightRegistry.markClosePageLogsAttached(page)) {
            attachConsoleMessages(CONSOLE_MESSAGES, page);
            attachPageErrors(PAGE_ERRORS, page);
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    private static void attachCloseTraces(final BrowserContext context) {
        for (DefaultTraceSession traceSession : AllurePlaywrightRegistry.getTraceSessions(context)) {
            traceSession.attach();
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    private static void attachFailureScreenshots() {
        for (Page page : AllurePlaywrightRegistry.getPages()) {
            attachScreenshot(SCREENSHOT, page);
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    private static void attachFailurePageSources() {
        for (Page page : AllurePlaywrightRegistry.getPages()) {
            attachPageSource(PAGE_SOURCE, page);
        }
    }

    @SuppressWarnings("PMD.CloseResource")
    private static void attachFailureTraces() {
        for (DefaultTraceSession traceSession : AllurePlaywrightRegistry.getTraceSessions()) {
            traceSession.attach();
        }
    }

    private static List<BrowserContext> contexts(final Browser browser) {
        try {
            return browser.contexts();
        } catch (RuntimeException e) {
            LOGGER.warn("Could not collect Playwright browser contexts", e);
            return Collections.emptyList();
        }
    }

    private static List<Page> pages(final BrowserContext context) {
        try {
            return context.pages();
        } catch (RuntimeException e) {
            LOGGER.warn("Could not collect Playwright pages", e);
            return Collections.emptyList();
        }
    }

    private static Video video(final Page page) {
        try {
            return page.video();
        } catch (RuntimeException e) {
            LOGGER.warn("Could not collect Playwright video", e);
            return null;
        }
    }

    private static AttachmentType videoType(final Path path) {
        if (path == null) {
            return AttachmentType.WEBM;
        }
        final String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".webm")) {
            return AttachmentType.WEBM;
        }
        return AttachmentType.OCTET_STREAM;
    }

    private static void attachPath(final String name, final AttachmentType type, final Path path) {
        if (path == null || !hasAllureContext() || !Files.isRegularFile(path)) {
            return;
        }
        try {
            attachBytes(name, type, Files.readAllBytes(path));
        } catch (IOException e) {
            LOGGER.warn("Could not attach Playwright artifact {}", path, e);
        }
    }

    private static void attachBytes(final String name, final AttachmentType type, final byte[] bytes) {
        if (bytes == null || !hasAllureContext()) {
            return;
        }
        final AllureLifecycle lifecycle = Allure.getLifecycle();
        lifecycle.addAttachment(name, type.getMediaType(), type.getExtension(), bytes);
    }

    private static void attachText(final String name, final String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        attachBytes(name, AttachmentType.TEXT, content.getBytes(StandardCharsets.UTF_8));
    }

    private static String defaultName(final String name, final String fallback) {
        return name == null || name.isEmpty() ? fallback : name;
    }

    private static boolean isJpegType(final Object options) {
        final Object type = readField(options, "type");
        return type != null && "JPEG".equals(type.toString());
    }

    private static boolean isJpegPath(final Object options) {
        final Object path = readField(options, "path");
        if (!(path instanceof Path)) {
            return false;
        }
        final String name = ((Path) path).getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg");
    }

    private static Object readField(final Object target, final String name) {
        if (target == null) {
            return null;
        }
        try {
            final Field field = target.getClass().getField(name);
            return field.get(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static String formatConsoleMessages(final List<ConsoleMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        final StringBuilder builder = new StringBuilder();
        for (ConsoleMessage message : messages) {
            final String type = safe(new Supplier<String>() {
                @Override
                public String get() {
                    return message.type();
                }
            });
            final String text = safe(new Supplier<String>() {
                @Override
                public String get() {
                    return message.text();
                }
            });
            final String location = safe(new Supplier<String>() {
                @Override
                public String get() {
                    return message.location();
                }
            });
            builder.append('[').append(type).append("] ").append(text);
            if (!location.isEmpty()) {
                builder.append(" (").append(location).append(')');
            }
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    private static String formatLines(final List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        final StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private static String safe(final Supplier<String> supplier) {
        try {
            final String value = supplier.get();
            return value == null ? "" : value;
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    static final class CloseArtifacts {

        private final List<Video> videos = new ArrayList<>();

        private void addVideo(final Video video) {
            if (video != null) {
                videos.add(video);
            }
        }

        void attachAfterClose() {
            if (!AllurePlaywrightConfig.shouldAttachCloseVideo()) {
                return;
            }
            for (Video video : videos) {
                attachVideo(VIDEO, path(video));
            }
        }

        private static Path path(final Video video) {
            try {
                return video.path();
            } catch (RuntimeException e) {
                LOGGER.warn("Could not resolve Playwright video path", e);
                return null;
            }
        }
    }
}
