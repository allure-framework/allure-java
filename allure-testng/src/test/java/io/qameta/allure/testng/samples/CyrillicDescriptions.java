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

import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class CyrillicDescriptions {

    @Test(testName = "Тест с описанием на русском языке только в testName")
    public void testWithCyrillicTestName() throws Exception {
    }

    @Test(description = "Тест с описанием на русском языке только в description")
    public void testWithCyrillicDescription() throws Exception {
    }

    @Test(testName = "Тест с описанием на русском языке и в testName",
            description = "Тест с описанием на русском языке и в description")
    public void testWithCyrillicTestNameAndDescription() throws Exception {
    }
}
