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

import org.testng.SkipException;
import org.testng.annotations.Test;

import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class TestsWithSteps {

    @Test
    public void testWithOneStep() {
        step("Sample step one");
    }

    @Test
    public void failingByAssertion() {
        step("Sample step one");
        step("Failing step", () -> {
            assertThat(2).isEqualTo(1);
        });
    }

    @Test
    public void skipped() {
        step("Sample step one");
        step("skipThisTest", () -> {
            throw new SkipException("Skipped");
        });
    }

    @Test
    public void brokenTest() {
        step("Sample step one");
        step("broken", () -> {
            throw new RuntimeException("Exception");
        });
    }

    @Test
    public void brokenTestWithoutMessage() {
        step("Sample step one");
        step("brokenWithoutMessage", () -> {
            throw new RuntimeException();
        });
    }
}
