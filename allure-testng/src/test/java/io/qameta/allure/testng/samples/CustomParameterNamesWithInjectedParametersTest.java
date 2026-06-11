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

import io.qameta.allure.Param;
import io.qameta.allure.model.Parameter;
import org.testng.ITestContext;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.xml.XmlTest;

import java.lang.reflect.Method;

public class CustomParameterNamesWithInjectedParametersTest {

    @Parameters({"first", "second", "third", "fourth"})
    @Test
    public void mixedInjectedParameters(final Method method,
                                        @Param("First") final String first,
                                        final ITestContext context,
                                        @Param(
                                                name = "Second",
                                                excluded = true,
                                                mode = Parameter.Mode.HIDDEN
                                        ) final String second,
                                        final XmlTest xmlTest,
                                        @Param(
                                                name = " ",
                                                mode = Parameter.Mode.MASKED
                                        ) final String third,
                                        final String fourth) {
    }

}
