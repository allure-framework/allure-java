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

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class FailedBeforeClassWithDataProvider {

    /**
     * Set to run the same suite with a passing fixture, so that the results of a skipped run and of a real run can
     * be compared.
     */
    public static final String FAIL_SETUP_PROPERTY = "allure.testng.sample.failBeforeClass";

    @BeforeClass
    public void setUp() {
        if (Boolean.getBoolean(FAIL_SETUP_PROPERTY)) {
            throw new RuntimeException("before class failed");
        }
    }

    @DataProvider(name = "rows")
    public Object[][] rows() {
        return new Object[][]{{"a"}, {"b"}, {"c"}};
    }

    @Test(dataProvider = "rows")
    public void parameterisedTest(final String value) {
    }

    @Test
    public void plainTest() {
    }

}
