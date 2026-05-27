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
package io.qameta.allure.testng.config;

import io.qameta.allure.util.PropertiesUtils;

import java.util.Properties;

import static java.lang.Boolean.parseBoolean;

/**
 * Stores configuration options for the Allure TestNG integration.
 *
 * <p>Create an instance from {@link java.util.Properties} when tests need explicit options, or use {@link #loadConfigProperties()} to read the standard Allure properties file. The fluent setters support programmatic configuration.</p>
 */
public class AllureTestNgConfig {

    /**
     * Configuration key for allure testng hide disabled tests.
     */
    public static final String ALLURE_TESTNG_HIDE_DISABLED_TESTS = "allure.testng.hide.disabled.tests";

    /**
     * Configuration key for allure testng hide configuration failures.
     */
    public static final String ALLURE_TESTNG_HIDE_CONFIGURATION_FAILURES = "allure.testng.hide.configuration.failures";
    private boolean hideDisabledTests;
    private boolean hideConfigurationFailures;

    /**
     * Creates an Allure test ng config with the supplied values.
     *
     * @param properties the properties to read configuration values from
     */
    public AllureTestNgConfig(final Properties properties) {
        this.hideDisabledTests = parseBoolean(properties.getProperty(ALLURE_TESTNG_HIDE_DISABLED_TESTS));
        this.hideConfigurationFailures = parseBoolean(
                properties.getProperty(ALLURE_TESTNG_HIDE_CONFIGURATION_FAILURES)
        );
    }

    /**
     * Returns whether hide disabled tests.
     *
     * @return true when hide disabled tests; false otherwise
     */
    public boolean isHideDisabledTests() {
        return hideDisabledTests;
    }

    /**
     * Sets the hide disabled tests.
     *
     * @param hide whether disabled tests should be hidden
     * @return this instance for method chaining
     */
    public AllureTestNgConfig setHideDisabledTests(final boolean hide) {
        this.hideDisabledTests = hide;
        return this;
    }

    /**
     * Returns whether hide configuration failures.
     *
     * @return true when hide configuration failures; false otherwise
     */
    public boolean isHideConfigurationFailures() {
        return hideConfigurationFailures;
    }

    /**
     * Sets the hide configuration failures.
     *
     * @param hideConfigurationFailure whether configuration failures should be hidden
     * @return this instance for method chaining
     */
    public AllureTestNgConfig setHideConfigurationFailures(final boolean hideConfigurationFailure) {
        this.hideConfigurationFailures = hideConfigurationFailure;
        return this;
    }

    /**
     * Loads and returns the config properties.
     *
     * @return the configuration loaded from standard Allure properties
     */
    public static AllureTestNgConfig loadConfigProperties() {
        final Properties properties = PropertiesUtils.loadAllureProperties();
        return new AllureTestNgConfig(properties);
    }

}
