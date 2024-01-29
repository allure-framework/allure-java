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
package io.qameta.allure.cucumber7jvm.samples;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * @author charlie (Dmitry Baev).
 */
public class AmbigiousSteps {

    @When("^ambigious step (.+)$")
    public void ambigious_1() {
        // nothing here
    }

    @When("^ambigious step ([a-z]+)$")
    public void ambigious_2() {
        // nothing here
    }

    @Then("^something bad should happen")
    public void somethingBadStep(){
        //nothing here
    }
}
