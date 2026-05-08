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

import io.qameta.allure.util.PropertiesUtils;

import java.util.Locale;
import java.util.Properties;

final class AllurePlaywrightConfig {

    static final String STEPS_ENABLED = "allure.playwright.steps.enabled";
    static final String STEPS_MODE = "allure.playwright.steps.mode";
    static final String PARAMETERS = "allure.playwright.parameters";
    static final String SCREENSHOTS_ATTACH = "allure.playwright.screenshots.attach";
    static final String FAILURE_SCREENSHOT = "allure.playwright.failure.screenshot";
    static final String FAILURE_PAGE_SOURCE = "allure.playwright.failure.page-source";
    static final String CLOSE_TRACE = "allure.playwright.close.trace";
    static final String CLOSE_VIDEO = "allure.playwright.close.video";
    static final String CLOSE_PAGE_LOGS = "allure.playwright.close.page-logs";

    private static final String ACTIONS = "actions";
    private static final String ALL = "all";
    private static final String REDACTED = "redacted";

    private AllurePlaywrightConfig() {
    }

    static boolean isStepsEnabled() {
        return getBoolean(STEPS_ENABLED, true);
    }

    static boolean isActionsMode() {
        return ACTIONS.equals(getString(STEPS_MODE, ACTIONS));
    }

    static boolean isAllMode() {
        return ALL.equals(getString(STEPS_MODE, ACTIONS));
    }

    static boolean isParametersRedacted() {
        return REDACTED.equals(getString(PARAMETERS, REDACTED));
    }

    static boolean shouldAttachScreenshots() {
        return getBoolean(SCREENSHOTS_ATTACH, true);
    }

    static boolean shouldAttachFailureScreenshot() {
        return getBoolean(FAILURE_SCREENSHOT, true);
    }

    static boolean shouldAttachFailurePageSource() {
        return getBoolean(FAILURE_PAGE_SOURCE, true);
    }

    static boolean shouldAttachCloseTrace() {
        return getBoolean(CLOSE_TRACE, true);
    }

    static boolean shouldAttachCloseVideo() {
        return getBoolean(CLOSE_VIDEO, true);
    }

    static boolean shouldAttachClosePageLogs() {
        return getBoolean(CLOSE_PAGE_LOGS, true);
    }

    private static boolean getBoolean(final String key, final boolean defaultValue) {
        return Boolean.parseBoolean(getProperties().getProperty(key, Boolean.toString(defaultValue)));
    }

    private static String getString(final String key, final String defaultValue) {
        return getProperties()
                .getProperty(key, defaultValue)
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static Properties getProperties() {
        return PropertiesUtils.loadAllureProperties();
    }

}
