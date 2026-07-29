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
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class SuccessPercentageTest {

    private static final AtomicInteger INVOCATIONS = new AtomicInteger();

    public static void resetInvocations() {
        INVOCATIONS.set(0);
    }

    @Test(
            invocationCount = 3,
            successPercentage = 66
    )
    public void succeedsWithinPercentage() {
        final int invocation = INVOCATIONS.incrementAndGet();
        Allure.parameter("invocation", invocation);
        if (invocation == 1) {
            throw new AssertionError("failed within success percentage");
        }
    }
}
