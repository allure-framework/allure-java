/*
 *  Copyright 2020 Qameta Software OÜ
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
package io.qameta.allure.junit5.features;

import io.qameta.allure.Allure;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllFixtureSupport {

    @BeforeAll
    static void setUpAll() {
        Allure.step("setUpAll 1");
        Allure.step("setUpAll 2");
    }

    @Test
    void test1() {
        Allure.step("test1 1");
        Allure.step("test1 2");
    }

    @AfterAll
    static void tearDownAll() {
        Allure.step("tearDownAll 1");
        Allure.step("tearDownAll 2");
    }
}
