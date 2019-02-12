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
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class PerSuiteFixtures {

    @BeforeSuite
    public void beforeSuite1() {
        step();
    }

    @BeforeSuite
    public void beforeSuite2() {
        step();
    }

    @Test
    public void test() {
        step();
    }

    @AfterSuite
    public void afterSuite1() {
        step();
    }

    @AfterSuite
    public void afterSuite2() {
        step();
    }

    @Step
    public void step(){

    }
}
