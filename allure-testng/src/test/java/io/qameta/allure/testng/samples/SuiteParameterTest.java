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

/*
  @author Andrejs Kalnacs akalnacs@evolutiongaming.com
 */
import org.testng.ITestContext;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class SuiteParameterTest {
    @Parameters("parameter")
    @BeforeSuite
    public void beforeSuite(String parameter, ITestContext context) {
        context.getCurrentXmlTest().addParameter("param", parameter);
    }

    @Test()
    public void simpleTest() {
    }
}
