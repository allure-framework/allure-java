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

import org.testng.IDataProviderMethod;
import org.testng.IRetryDataProvider;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class FlakyDataProvider {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    @BeforeClass
    public void reset() {
        COUNTER.set(0);
    }

    public static class Retry implements IRetryDataProvider {
        @Override
        public boolean retry(IDataProviderMethod method) {
            return COUNTER.incrementAndGet() < 2;
        }
    }

    @DataProvider(retryUsing = Retry.class)
    public Object[][] provide() {
        if (COUNTER.get() == 0) {
            throw new RuntimeException("Simulated DataProvider Failure");
        }
        return new Object[][]{{"data"}};
    }

    @Test(dataProvider = "provide")
    public void test(String s) {
    }
}
