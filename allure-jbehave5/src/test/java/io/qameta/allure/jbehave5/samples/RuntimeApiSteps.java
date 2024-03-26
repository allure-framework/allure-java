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
package io.qameta.allure.jbehave5.samples;

import io.qameta.allure.Allure;
import org.jbehave.core.annotations.Given;

public class RuntimeApiSteps {

    @Given("runtime api")
    public void given() {
        Allure.label("jbehave-test-label", "some-value");
        Allure.parameter("test param", "param value");
        Allure.step("sub step 1");
        Allure.step("sub step 2", () -> {
        });

        Allure.addAttachment("some attachment", "some content");
    }

}
