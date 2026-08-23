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
package io.qameta.allure.junitplatform.features;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * The class from issue #1155: {@code @BeforeAll} fails on the first attempt and succeeds on the retry.
 * It has to be one class with a toggle rather than two fixtures, because only the same class produces
 * the same history ids.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RetryBeforeAllTests {

    public static boolean beforeAllShouldFail = true;

    @BeforeAll
    void beforeAll() {
        if (beforeAllShouldFail) {
            throw new RuntimeException("Simulated failure in @BeforeAll");
        }
    }

    @Test
    void test1() {
    }

    @Test
    void test2() {
    }

    @Test
    void test3() {
    }
}
