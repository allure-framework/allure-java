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
package io.qameta.allure.testng7.samples;

import io.qameta.allure.testng7.TestInstanceParameter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class FactoryOnConstructorTests {

    @TestInstanceParameter
    private int number;

    @TestInstanceParameter("Name")
    private String v;

    private Long other;

    @Factory(dataProvider = "dataProvider")
    public FactoryOnConstructorTests(final int number, final String v, final Long other) {
        this.number = number;
        this.v = v;
        this.other = other;
    }

    @DataProvider
    public static Object[][] dataProvider() {
        return new Object[][]{new Object[]{1, "first", 22L}, new Object[]{2, "second", 23L}};
    }

    @Test
    public void factoryTest() {
    }
}
