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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Make sure data driven tests work when the suite runs with
 * {@code use-global-thread-pool="true"} (see allure-java#1013).
 */
public class GlobalThreadPoolSample {

    private static final int DATA_SIZE = 25;

    @Test(dataProvider = "dp")
    public void firstParallelDataDrivenTest(final int value) {
    }

    @Test(dataProvider = "dp")
    public void secondParallelDataDrivenTest(final int value) {
    }

    @DataProvider(
            name = "dp",
            parallel = true
    )
    public Object[][] provide() {
        final Object[][] data = new Object[DATA_SIZE][1];
        for (int i = 0; i < DATA_SIZE; i++) {
            data[i][0] = i;
        }
        return data;
    }
}
