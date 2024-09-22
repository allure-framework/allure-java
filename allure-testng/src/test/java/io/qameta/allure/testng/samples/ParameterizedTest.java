/*
 *  Copyright 2016-2024 Qameta Software Inc
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

import io.qameta.allure.Param;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static io.qameta.allure.Allure.step;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class ParameterizedTest {

    @BeforeMethod
    public void beforeMethod() {

    }

    @DataProvider
    public Object[][] testData() {
        return new String[][]{
                {"param1"},
                {"param2"}
        };
    }

    @DataProvider
    public static Object[][] testDataForParamNames() {
        return new Object[][]{
            {1, 1, 2, 5},
            {2, 2, 4, 5}
        };
    }

    @Test(dataProvider = "testData")
    public void parameterizedTest(String param) {
        step(param);
    }

    @Test(dataProvider = "testDataForParamNames")
    public void sumTest(
        @Param(name = "First") Integer a,
        @Param(name = "Second") Integer b,
        @Param(name = "Third") Integer r,
        @Param(name = "Fourth") Integer s) {

        step(("Arrange"), () -> {
            step(String.format("Take collection №[%s] of parameters", a));
        });
        step(("Act"), () -> {
            step(String.format("Add [%s]", a) + String.format("to [%s]", b));
        });
        step(("Assert"), () -> {
            step("Compare the sum");
            assert a + b == r;
        });

    }
}
