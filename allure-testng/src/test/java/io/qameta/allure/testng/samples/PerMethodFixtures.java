/*
 *  Copyright 2019 Qameta Software OÜ
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

import io.qameta.allure.Step;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class PerMethodFixtures {

    @BeforeMethod
    public void beforeMethod1() {
        step();
    }

    @BeforeMethod
    public void beforeMethod2() {
        step();
    }

    @Test
    public void test1() {
        step();
    }

    @Test
    public void test2() {
        step();
    }

    @AfterMethod
    public void afterMethod1() {
        step();
    }

    @AfterMethod
    public void afterMethod2() {
        step();
    }

    @Step
    public void step() {

    }
}
