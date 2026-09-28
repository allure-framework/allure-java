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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks {@link AllurePlaywright#shouldEmbedSources()} for the case that actually matters: relies on this
 * test environment not having {@code PLAYWRIGHT_JAVA_SRC} set (there's no supported way to fake that from
 * inside the JVM), and flips {@code allure.playwright.trace.sources} on via a system property, which
 * {@code PropertiesUtils} reads on top of {@code allure.properties}.
 */
class AllurePlaywrightEmbedSourcesTest {

    private static final String TRACE_SOURCES = "allure.playwright.trace.sources";

    @AfterEach
    void clearProperty() {
        System.clearProperty(TRACE_SOURCES);
    }

    @Test
    void shouldNotEmbedWhenJavaSrcEnvVarIsNotSet() {
        System.setProperty(TRACE_SOURCES, "true");

        assertThat(AllurePlaywright.shouldEmbedSources()).isFalse();
    }
}
