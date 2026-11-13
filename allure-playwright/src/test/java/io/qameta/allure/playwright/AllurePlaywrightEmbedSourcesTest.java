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
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.test.AllureResults;
import io.qameta.allure.test.IsolatedLifecycle;
import io.qameta.allure.test.RunUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;

/**
 * Checks source embedding through the public {@link AllurePlaywright#startTracing(String, BrowserContext)} helper
 * when {@code PLAYWRIGHT_JAVA_SRC} is configured per Playwright instance via {@code CreateOptions.setEnv()} rather
 * than in the JVM process environment.
 */
@ResourceLock(
        value = "io.qameta.allure.playwright.lifecycle",
        mode = READ_WRITE
)
@IsolatedLifecycle
class AllurePlaywrightEmbedSourcesTest {

    private static final String TRACE_SOURCES = "allure.playwright.trace.sources";

    @AfterEach
    void clearProperty() {
        System.clearProperty(TRACE_SOURCES);
        AllurePlaywright.clear();
    }

    @Test
    void shouldEmbedSourcesWithPerInstanceJavaSrcConfiguration(@TempDir final Path tempDir) throws IOException {
        System.setProperty(TRACE_SOURCES, "true");
        final Path testSrcRoot = Path.of("src", "test", "java").toAbsolutePath();

        final AllureResults results;
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
            final Page page = context.newPage();
            results = RunUtils.runTests(lifecycle -> {
                final AllureExternalKey testKey = AllureExternalKey.random(AllurePlaywrightEmbedSourcesTest.class);
                lifecycle.scheduleTest(testKey, new TestResult().setUuid(UUID.randomUUID().toString()));
                lifecycle.startTest(testKey);
                try (TraceSession traceSession = AllurePlaywright.startTracing("Trace", context)) {
                    page.setContent("<button>Trace</button>");
                    page.click("button");
                } finally {
                    lifecycle.stopTest(testKey);
                    lifecycle.writeTest(testKey);
                }
            });
            context.close();
            browser.close();
        }

        assertThat(results.getAttachments()).hasSize(1);
        final Path trace = tempDir.resolve("trace.zip");
        Files.write(trace, results.getAttachments().values().iterator().next());

        try (ZipFile zip = new ZipFile(trace.toFile())) {
            final byte[] testSource = Files.readAllBytes(
                    testSrcRoot.resolve("io/qameta/allure/playwright/AllurePlaywrightEmbedSourcesTest.java")
            );
            assertThat(zip.stream().map(entry -> readEntry(zip, entry)))
                    .as("embedded source of this test class")
                    .anySatisfy(content -> assertThat(content).isEqualTo(testSource));
            final ZipEntry stacksEntry = zip.getEntry("trace.stacks");
            assertThat(stacksEntry).as("trace.stacks entry").isNotNull();
            final JsonObject stacksJson;
            try (InputStream in = zip.getInputStream(stacksEntry)) {
                stacksJson = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                        .getAsJsonObject();
            }
            final JsonArray files = stacksJson.getAsJsonArray("files");
            final JsonArray stacks = stacksJson.getAsJsonArray("stacks");
            assertThat(stacks).isNotEmpty();
            for (JsonElement stackElement : stacks) {
                final JsonArray frame0 = stackElement.getAsJsonArray().get(1).getAsJsonArray()
                        .get(0).getAsJsonArray();
                assertThat(frame0.get(3).getAsString())
                        .doesNotContain("JoinPointImpl", "AllurePlaywrightAspect", "AjcClosure");
                assertThat(files.get(frame0.get(0).getAsInt()).getAsString())
                        .endsWith("AllurePlaywrightEmbedSourcesTest.java");
            }
        }
    }

    private static byte[] readEntry(final ZipFile zip, final ZipEntry entry) {
        try (InputStream in = zip.getInputStream(entry)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
