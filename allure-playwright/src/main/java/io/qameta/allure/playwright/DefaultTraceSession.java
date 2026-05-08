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

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Tracing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class DefaultTraceSession implements TraceSession {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTraceSession.class);

    private final BrowserContext context;
    private final String name;
    private boolean stopped;

    DefaultTraceSession(final BrowserContext context, final String name) {
        this.context = context;
        this.name = name;
    }

    @Override
    public void attach() {
        stop(true);
    }

    @Override
    public void close() {
        attach();
    }

    void stopWithoutAttachment() {
        stop(false);
    }

    @SuppressWarnings("PMD.CompareObjectsWithEquals")
    boolean isFor(final BrowserContext candidate) {
        return context == candidate;
    }

    private void stop(final boolean attach) {
        if (stopped) {
            return;
        }
        stopped = true;
        Path trace = null;
        try {
            trace = Files.createTempFile("allure-playwright-trace-", ".zip");
            context.tracing().stop(new Tracing.StopOptions().setPath(trace));
            if (attach) {
                AllurePlaywright.attachTrace(name, trace);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not stop Playwright tracing", e);
        } finally {
            delete(trace);
        }
    }

    private static void delete(final Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LOGGER.debug("Could not delete temporary Playwright trace {}", path, e);
        }
    }
}
