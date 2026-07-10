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

import io.qameta.allure.testng.TestInstanceParameter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

public class TestInstanceParameterInheritanceTests extends TestInstanceParameterBase {

    @TestInstanceParameter("overridden")
    private final String overridden = "child-value";

    @Factory(dataProvider = "instances")
    public TestInstanceParameterInheritanceTests(final String iteration, final String hidden) {
        super(iteration, hidden);
    }

    @DataProvider
    public static Object[][] instances() {
        return new Object[][]{
                new Object[]{"first", "hidden-value"},
                new Object[]{"second", "hidden-value"},
                new Object[]{"third", "different-hidden-value"},
        };
    }

    @Test
    public void factoryTest() {
    }

}
