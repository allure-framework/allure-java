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

import org.testng.ITestResult;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.util.RetryAnalyzerCount;

import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;

public class RetriedFlakyTest {

    private static final AtomicInteger ATTEMPTS = new AtomicInteger();

    @BeforeClass
    public void resetAttempts() {
        ATTEMPTS.set(0);
    }

    @Test(retryAnalyzer = RetryOnce.class)
    public void passesOnRetry() {
        assertEquals(ATTEMPTS.incrementAndGet(), 2, "Only the retry passes");
    }

    public static class RetryOnce extends RetryAnalyzerCount {

        @Override
        public boolean retryMethod(final ITestResult result) {
            return !result.isSuccess();
        }
    }
}
