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
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Reproduces the popular screenshot-on-failure pattern: a custom listener registered via
 * {@link Listeners} that reports evidence through the Allure API from test listener callbacks.
 */
@Listeners(CustomListenerAttachments.EvidenceListener.class)
public class CustomListenerAttachments {

    @Test
    public void passingTest() {
    }

    @Test
    public void failingTest() {
        throw new AssertionError("failed on purpose");
    }

    public static class EvidenceListener implements ITestListener {

        @Override
        public void onTestStart(final ITestResult result) {
            Allure.attachment("On test start", "text/plain", "before " + result.getName());
        }

        @Override
        public void onTestSuccess(final ITestResult result) {
            Allure.attachment("On test success", "text/plain", "after passed " + result.getName());
        }

        @Override
        public void onTestFailure(final ITestResult result) {
            Allure.step("Take screenshot on failure");
            Allure.attachment("Screenshot on failure", "text/plain", "after failed " + result.getName());
        }
    }
}
