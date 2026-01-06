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

import io.qameta.allure.Description;
import io.qameta.allure.Step;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Sergey Potanin sspotanin@gmail.com
 */
public class DescriptionsAnotherTest {

    /**
     * Before class description from DescriptionsAnotherTest
     */
    @BeforeClass
    @Description(useJavaDoc = true)
    public void setUpClass() {

    }

    /**
     * Before method description from DescriptionsAnotherTest
     */
    @BeforeMethod
    @Description(useJavaDoc = true)
    public void setUpMethod() {

    }

    /**
     * Sample test description from DescriptionsAnotherTest
     */
    @Description(useJavaDoc = true)
    @Test
    public void test() {
        step();
    }

    /**
     * Sample test description from DescriptionsAnotherTest
     * - next line
     * - another line
     */
    @Description(useJavaDoc = true)
    @Test
    public void testSeparated() {
        step();
    }

    /**
     * Sample step description from DescriptionsAnotherTest
     */
    @Description(useJavaDoc = true)
    @Step("Step one")
    private void step() {
    }

}
