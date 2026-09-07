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
package io.qameta.allure.testng.samples;

import io.qameta.allure.Allure;
import org.testng.IConfigurationListener;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class ConfigurationListenerFixtures {

    public static class Passing {
        @BeforeSuite
        public void beforeSuite() {
        }

        @BeforeMethod
        public void beforeMethod() {
        }

        @Test
        public void test() {
            Allure.step("Test body");
        }

        @AfterMethod
        public void afterMethod() {
        }

        @AfterSuite
        public void afterSuite() {
        }
    }

    @Listeners(EvidenceListener.class)
    public static class Annotated extends Passing {
    }

    public static class Skipped {
        @BeforeMethod
        public void skip() {
            throw new SkipException("Configuration intentionally skipped");
        }

        @BeforeMethod(dependsOnMethods = "skip")
        public void skippedDependency() {
        }

        @Test
        public void test() {
        }
    }

    public static class FirstTimeOnly {
        @BeforeMethod(firstTimeOnly = true)
        public void beforeMethod() {
            Allure.attachment("Setup body", "first invocation only");
        }

        @Test(invocationCount = 2)
        public void test() {
            Allure.step("Test body");
        }
    }

    public static class MissingParameter {
        @BeforeMethod
        @Parameters("missing")
        public void beforeMethod(final String value) {
        }

        @Test
        public void test() {
        }

        @AfterMethod(alwaysRun = true)
        public void afterMethod() {
        }
    }

    public static class EvidenceListener implements IConfigurationListener {
        @Override
        public void onConfigurationSuccess(final ITestResult result) {
            Allure.attachment("Configuration success", result.getName());
        }

        @Override
        public void onConfigurationFailure(final ITestResult result) {
            Allure.attachment("Configuration failure", result.getName());
        }

        @Override
        public void onConfigurationSkip(final ITestResult result) {
            Allure.attachment("Configuration skip", result.getName());
        }
    }

    public static class AnotherListener implements IConfigurationListener {
        @Override
        public void onConfigurationSuccess(final ITestResult result) {
            Allure.attachment("Another listener", result.getName());
        }
    }
}
