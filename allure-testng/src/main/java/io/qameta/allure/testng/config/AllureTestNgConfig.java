/*
 *  Copyright 2016-2024 Qameta Software Inc
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
package io.qameta.allure.testng.config;

import io.qameta.allure.util.PropertiesUtils;

import java.util.Properties;

import static java.lang.Boolean.parseBoolean;

public class AllureTestNgConfig {

    public static final String ALLURE_TESTNG_HIDE_DISABLED_TESTS = "allure.testng.hide.disabled.tests";
    public static final String ALLURE_TESTNG_HIDE_CONFIGURATION_FAILURES = "allure.testng.hide.configuration.failures";
    private boolean hideDisabledTests;
    private boolean hideConfigurationFailures;

    public AllureTestNgConfig(final Properties properties) {
        this.hideDisabledTests = parseBoolean(properties.getProperty(ALLURE_TESTNG_HIDE_DISABLED_TESTS));
        this.hideConfigurationFailures = parseBoolean(
            properties.getProperty(ALLURE_TESTNG_HIDE_CONFIGURATION_FAILURES)
        );
    }

    public boolean isHideDisabledTests() {
        return hideDisabledTests;
    }

    public AllureTestNgConfig setHideDisabledTests(final boolean hide) {
        this.hideDisabledTests = hide;
        return this;
    }

    public boolean isHideConfigurationFailures() {
        return hideConfigurationFailures;
    }

    public AllureTestNgConfig setHideConfigurationFailures(final boolean hideConfigurationFailure) {
        this.hideConfigurationFailures = hideConfigurationFailure;
        return this;
    }

    public static AllureTestNgConfig loadConfigProperties() {
        final Properties properties = PropertiesUtils.loadAllureProperties();
        return new AllureTestNgConfig(properties);
    }

}
