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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Tracing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Checks {@link TraceStackSourceTrimmer}'s pure {@code trace.stacks} transform directly against fixture payloads,
 * plus one end-to-end capture against a real trace to confirm the fix holds against the actual format Playwright
 * writes, not just an assumed one.
 */
class TraceStackSourceTrimmerTest {

    /**
     * Checks that a run of synthetic AspectJ/Playwright-weaving frames in front of a real caller is trimmed.
     */
    @Test
    void shouldTrimLeadingSyntheticFrames() {
        final byte[] input = ("{"
                + "\"files\":[\"\",\"/src/Caller.java\"],"
                + "\"stacks\":[[1,["
                + "[0,164,0,\"org.aspectj.runtime.reflect.JoinPointImpl.proceed\"],"
                + "[0,119,0,\"io.qameta.allure.playwright.AllurePlaywrightAspect.logPlaywrightStep\"],"
                + "[0,723,0,\"com.microsoft.playwright.impl.PageImpl.click\"],"
                + "[0,6,0,\"com.microsoft.playwright.Page.click_aroundBody6\"],"
                + "[0,1,0,\"com.microsoft.playwright.Page$AjcClosure7.run\"],"
                + "[1,34,0,\"com.example.Caller.run\"],"
                + "[0,565,0,\"java.lang.reflect.Method.invoke\"]"
                + "]]]}").getBytes(StandardCharsets.UTF_8);

        final JsonArray frames = trimAndGetFrames(input, 1);

        assertThat(frames).hasSize(2);
        assertThat(frames.get(0).getAsJsonArray().get(3).getAsString()).isEqualTo("com.example.Caller.run");
        assertThat(frames.get(1).getAsJsonArray().get(3).getAsString()).isEqualTo("java.lang.reflect.Method.invoke");
    }

    /**
     * Checks that a stack whose first frame is already file-resolvable is left untouched, even when later frames
     * would otherwise match the synthetic-frame denylist.
     */
    @Test
    void shouldLeaveAlreadyResolvedStackUnchanged() {
        final byte[] input = ("{"
                + "\"files\":[\"/src/Caller.java\"],"
                + "\"stacks\":[[1,["
                + "[0,10,0,\"com.example.Caller.run\"],"
                + "[-1,164,0,\"org.aspectj.runtime.reflect.JoinPointImpl.proceed\"]"
                + "]]]}").getBytes(StandardCharsets.UTF_8);

        final JsonArray frames = trimAndGetFrames(input, 1);

        assertThat(frames).hasSize(2);
        assertThat(frames.get(0).getAsJsonArray().get(3).getAsString()).isEqualTo("com.example.Caller.run");
    }

    /**
     * Checks that a stack with no file-resolvable frame and no non-synthetic frame is left untouched rather than
     * trimmed down to nothing — a safe no-op, matching what happens when a consumer enables {@code sources}
     * without ever configuring {@code PLAYWRIGHT_JAVA_SRC}.
     */
    @Test
    void shouldNotEmptyAFullyUnresolvedStack() {
        final byte[] input = ("{"
                + "\"files\":[\"\"],"
                + "\"stacks\":[[1,["
                + "[0,164,0,\"org.aspectj.runtime.reflect.JoinPointImpl.proceed\"],"
                + "[0,119,0,\"io.qameta.allure.playwright.AllurePlaywrightAspect.logPlaywrightStep\"]"
                + "]]]}").getBytes(StandardCharsets.UTF_8);

        final JsonArray frames = trimAndGetFrames(input, 1);

        assertThat(frames).hasSize(2);
    }

    /**
     * Checks that a stack with no synthetic frames at all (an unadvised call) is left completely unchanged.
     */
    @Test
    void shouldLeaveNonSyntheticStackUnchanged() {
        final byte[] input = ("{"
                + "\"files\":[\"\"],"
                + "\"stacks\":[[1,["
                + "[0,10,0,\"com.example.Caller.run\"],"
                + "[0,20,0,\"com.example.Other.helper\"]"
                + "]]]}").getBytes(StandardCharsets.UTF_8);

        final JsonArray frames = trimAndGetFrames(input, 1);

        assertThat(frames).hasSize(2);
        assertThat(frames.get(0).getAsJsonArray().get(3).getAsString()).isEqualTo("com.example.Caller.run");
    }

    /**
     * Checks that multiple stacks in the same payload are trimmed independently of each other.
     */
    @Test
    void shouldTrimEachStackIndependently() {
        final byte[] input = ("{"
                + "\"files\":[\"\",\"/src/Caller.java\"],"
                + "\"stacks\":["
                + "[1,[[0,164,0,\"org.aspectj.runtime.reflect.JoinPointImpl.proceed\"],[1,1,0,\"com.example.A.a\"]]],"
                + "[2,[[1,2,0,\"com.example.B.b\"]]]"
                + "]}").getBytes(StandardCharsets.UTF_8);

        final JsonObject root = JsonParser.parseString(
                new String(TraceStackSourceTrimmer.trimStacksJson(input), StandardCharsets.UTF_8)
        ).getAsJsonObject();
        final JsonArray stacks = root.getAsJsonArray("stacks");

        assertThat(stacks.get(0).getAsJsonArray().get(1).getAsJsonArray()).hasSize(1);
        assertThat(stacks.get(1).getAsJsonArray().get(1).getAsJsonArray()).hasSize(1);
    }

    /**
     * Checks that {@link TraceStackSourceTrimmer#trim(Path)} fails open: a file that isn't a valid zip at all is
     * left byte-for-byte untouched instead of being partially rewritten or deleted.
     *
     * @param tempDir a directory managed by JUnit for this test's temporary file.
     */
    @Test
    void shouldLeaveFileUntouchedWhenNotAValidZip(@TempDir final Path tempDir) throws IOException {
        final Path notAZip = tempDir.resolve("not-a-trace.zip");
        final byte[] original = "not actually a zip file".getBytes(StandardCharsets.UTF_8);
        Files.write(notAZip, original);

        TraceStackSourceTrimmer.trim(notAZip);

        assertThat(Files.readAllBytes(notAZip)).isEqualTo(original);
    }

    /**
     * Checks that a valid trace with no {@code trace.stacks} entry at all — what actually happens when
     * {@code PLAYWRIGHT_JAVA_SRC} was never configured, since Playwright's client then never collects a stack
     * per call — is left completely, byte-for-byte untouched, without paying for a full unzip/rewrite.
     *
     * <p>Byte-for-byte, not just functionally equivalent, is the point of this test: a full rewrite through
     * {@code ZipOutputStream} would re-derive entry metadata and almost certainly not reproduce the original
     * bytes exactly, so this also guards against the short-circuit in
     * {@link TraceStackSourceTrimmer#trim(Path)} silently regressing back into always rewriting.</p>
     *
     * @param tempDir a directory managed by JUnit for this test's temporary file.
     */
    @Test
    void shouldLeaveTraceUntouchedWhenNoStacksEntry(@TempDir final Path tempDir) throws IOException {
        final Path trace = tempDir.resolve("trace.zip");
        writeZip(trace, "trace.trace", "{\"type\":\"context-options\"}".getBytes(StandardCharsets.UTF_8));
        final byte[] original = Files.readAllBytes(trace);

        TraceStackSourceTrimmer.trim(trace);

        assertThat(Files.readAllBytes(trace)).isEqualTo(original);
    }

    /**
     * Checks that {@code resources/} entries (screenshots, snapshots — already-compressed-or-incompressible
     * binary blobs) are stored rather than re-compressed when a trace does get rewritten: content must round
     * trip exactly, and the entry should come out {@link ZipEntry#STORED} rather than
     * {@link ZipEntry#DEFLATED}, since re-deflating such content buys nothing but CPU time.
     *
     * @param tempDir a directory managed by JUnit for this test's temporary file.
     */
    @Test
    void shouldStoreResourceEntriesRatherThanRecompressThem(@TempDir final Path tempDir) throws IOException {
        final Path trace = tempDir.resolve("trace.zip");
        final byte[] screenshot = new byte[4096];
        new Random(42).nextBytes(screenshot);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(trace))) {
            zos.putNextEntry(new ZipEntry("resources/page@abc.jpeg"));
            zos.write(screenshot);
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("trace.stacks"));
            zos.write(
                    ("{\"files\":[\"\"],\"stacks\":[[1,[[0,1,0,\"com.example.Caller.run\"]]]]}")
                            .getBytes(StandardCharsets.UTF_8)
            );
            zos.closeEntry();
        }

        TraceStackSourceTrimmer.trim(trace);

        try (ZipFile zip = new ZipFile(trace.toFile())) {
            ZipEntry resourceEntry = zip.getEntry("resources/page@abc.jpeg");
            assertThat(resourceEntry.getMethod()).isEqualTo(ZipEntry.STORED);
            try (InputStream in = zip.getInputStream(resourceEntry)) {
                assertThat(in.readAllBytes()).isEqualTo(screenshot);
            }
        }
    }

    private static void writeZip(final Path zip, final String entryName, final byte[] content) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry(entryName));
            zos.write(content);
            zos.closeEntry();
        }
    }

    /**
     * End-to-end: captures a real trace from an aspect-advised {@code page.click()} call, with
     * {@code PLAYWRIGHT_JAVA_SRC} pointed at this test's own source root, trims it, and confirms frame 0 of
     * the click action's stack now resolves to this test class rather than the synthetic AspectJ frame.
     *
     * <p>Uses its own {@link Playwright} instance (rather than a shared one) because {@code PLAYWRIGHT_JAVA_SRC}
     * is only read once, at driver-connection time.</p>
     */
    @Test
    void shouldResolveRealCallerAfterTrimmingARealTrace(@TempDir final Path tempDir) throws IOException {
        final Path testSrcRoot = Path.of("src", "test", "java").toAbsolutePath();

        try (Playwright playwright = Playwright.create(
                new Playwright.CreateOptions()
                        .setEnv(Collections.singletonMap("PLAYWRIGHT_JAVA_SRC", testSrcRoot.toString()))
        )) {
            final Browser browser;
            try {
                browser = playwright.chromium().launch(
                        new BrowserType.LaunchOptions()
                                .setHeadless(true)
                                .setArgs(Collections.singletonList("--no-sandbox"))
                );
            } catch (PlaywrightException e) {
                assumeTrue(false, "Chromium is not available for Playwright integration tests: " + e.getMessage());
                return;
            }
            final BrowserContext context = browser.newContext();
            context.tracing().start(
                    new Tracing.StartOptions().setScreenshots(true).setSnapshots(true)
                            .setSources(true)
            );
            final Page page = context.newPage();
            page.setContent("<button>Trace</button>");
            page.click("button");

            final Path trace = tempDir.resolve("trace.zip");
            context.tracing().stop(new Tracing.StopOptions().setPath(trace));
            context.close();
            browser.close();

            TraceStackSourceTrimmer.trim(trace);

            final JsonObject stacksJson = readStacksEntry(trace);
            final JsonArray files = stacksJson.getAsJsonArray("files");
            final JsonArray stacks = stacksJson.getAsJsonArray("stacks");

            assertThat(stacks).isNotEmpty();
            for (JsonElement stackElement : stacks) {
                final JsonArray frame0 = stackElement.getAsJsonArray().get(1).getAsJsonArray()
                        .get(0).getAsJsonArray();
                final int fileIndex = frame0.get(0).getAsInt();
                final String name = frame0.get(3).getAsString();
                assertThat(name).doesNotContain("JoinPointImpl", "AllurePlaywrightAspect", "AjcClosure");
                if (fileIndex >= 0) {
                    assertThat(files.get(fileIndex).getAsString()).isNotEmpty();
                }
            }
        }
    }

    private static JsonArray trimAndGetFrames(final byte[] input, final int stackIndex) {
        final byte[] output = TraceStackSourceTrimmer.trimStacksJson(input);
        final JsonObject root = JsonParser.parseString(new String(output, StandardCharsets.UTF_8)).getAsJsonObject();
        for (JsonElement stackElement : root.getAsJsonArray("stacks")) {
            final JsonArray stackEntry = stackElement.getAsJsonArray();
            if (stackEntry.get(0).getAsInt() == stackIndex) {
                return stackEntry.get(1).getAsJsonArray();
            }
        }
        throw new AssertionError("No stack with id " + stackIndex);
    }

    private static JsonObject readStacksEntry(final Path trace) throws IOException {
        try (ZipFile zip = new ZipFile(trace.toFile())) {
            final ZipEntry entry = zip.getEntry("trace.stacks");
            assertThat(entry).as("trace.stacks entry").isNotNull();
            try (InputStream in = zip.getInputStream(entry)) {
                return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                        .getAsJsonObject();
            }
        }
    }
}
