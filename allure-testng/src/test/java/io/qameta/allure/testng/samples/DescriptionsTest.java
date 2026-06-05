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

import io.qameta.allure.Description;
import io.qameta.allure.Step;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
public class DescriptionsTest {

    /**
     * Initializes the TestNG sample class and verifies class fixture descriptions are available.
     */
    @BeforeClass
    @Description
    public void setUpClass() {

    }

    /**
     * Initializes each TestNG sample method and verifies method fixture descriptions are available.
     */
    @BeforeMethod
    @Description
    public void setUpMethod() {

    }

    /**
     * Runs a TestNG test that records a step through the sample fixture.
     */
    @Description
    @Test
    public void test() {
        step();
    }

    /**
     * Runs a TestNG test whose JavaDoc contains multiple summary lines.
     * - verifies the first summary line
     * - verifies the following list items
     */
    @Description
    @Test
    public void testSeparated() {
        step();
    }

    /**
     * Records the sample step used by the TestNG description fixtures.
     */
    @Description
    @Step("Step one")
    private void step() {
    }

}
