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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static io.qameta.allure.Allure.step;

/**
 * @author Sambhav Dave sambhavd4@gmail.com
 */
public class CustomParameterNamesTest {

    @DataProvider
    public static Object[][] testDataForParamNames() {
        return new Object[][]{
            {1, 1, 2, 5}
        };
    }

    @Test(dataProvider = "testDataForParamNames")
    public void sumTest(
        @Param(name = "First") Integer a,
        @Param(name = "Second") Integer b,
        @Param(name = "Third") Integer expectedSum,
        @Param(name = "Fourth") Integer unusedParam) {

        step("Arrange", () -> step(String.format("Use parameters: First = [%s], Second = [%s], Third = [%s], Fourth = [%s]",
            a, b, expectedSum, unusedParam)));

        Integer result = step("Act", () -> {
            step(String.format("Add First [%s] and Second [%s]", a, b));
            return a + b;
        });

        step("Assert", () -> {
            step(String.format("Compare result [%s] with expected [%s]", result, expectedSum));
            assert result.equals(expectedSum) : "Sum does not match the expected value";
        });
    }

}
