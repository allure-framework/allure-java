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

import java.util.concurrent.atomic.AtomicInteger;

import static io.qameta.allure.Allure.parameter;

public class PriorityTests {

    private final static String ORDER_PARAMETER = "order";

    private final AtomicInteger cnt = new AtomicInteger();

    @DataProvider(name = "someProvider")
    public static Object[][] someProvider() {
        return new Object[][]{
                new Object[]{1},
                new Object[]{2}
        };
    }

    @Test(dataProvider = "someProvider", priority = 4)
    public void vTest(int parameter) {
        parameter(ORDER_PARAMETER, cnt.incrementAndGet());
    }

    @Test(priority = 3)
    public void wTest() {
        parameter(ORDER_PARAMETER, cnt.incrementAndGet());
    }

    @Test(priority = 2)
    public void xTest() {
        parameter(ORDER_PARAMETER, cnt.incrementAndGet());
    }

    @Test(priority = 1)
    public void yTest() {
        parameter(ORDER_PARAMETER, cnt.incrementAndGet());
    }

    @Test
    public void zTest() {
        parameter(ORDER_PARAMETER, cnt.incrementAndGet());
    }

}
