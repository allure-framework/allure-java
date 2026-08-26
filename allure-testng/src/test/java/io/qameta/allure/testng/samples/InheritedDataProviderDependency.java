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
 * The data provider is declared on the class-level {@code @Test}, and inherited by the dependent method whose own
 * {@code @Test} annotation only declares the dependency.
 */
@Test(dataProvider = "rows")
public class InheritedDataProviderDependency {

    /**
     * Set to run the same suite with a passing upstream test, so that the results of a run whose dependency failed
     * and of a real run can be compared.
     */
    public static final String FAIL_UPSTREAM_PROPERTY = "allure.testng.sample.failInheritedUpstream";

    @DataProvider(name = "rows")
    public Object[][] rows() {
        return new Object[][]{{"a"}, {"b"}};
    }

    @DataProvider(name = "none")
    public Object[][] none() {
        return new Object[][]{{}};
    }

    @Test(dataProvider = "none")
    public void upstreamTest() {
        if (Boolean.getBoolean(FAIL_UPSTREAM_PROPERTY)) {
            throw new RuntimeException("upstream failed");
        }
    }

    @Test(dependsOnMethods = "upstreamTest")
    public void dependentInheritedTest(final String value) {
    }
}
