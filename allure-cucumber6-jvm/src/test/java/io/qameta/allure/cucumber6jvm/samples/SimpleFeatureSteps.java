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
package io.qameta.allure.cucumber6jvm.samples;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

/**
 * @author charlie (Dmitry Baev).
 */
public class SimpleFeatureSteps {

    private int a;
    private int b;
    private int c;

    @Given("^a is (\\d+)$")
    public void a_is(int arg1) {
        this.a = arg1;
    }

    @Given("^b is (\\d+)$")
    public void b_is(int arg1) {
        this.b = arg1;
    }

    @When("^I add a to b$")
    public void i_add_a_to_b() {
        this.c = this.a + this.b;
    }

    @Then("^result is (\\d+)$")
    public void result_is(int arg1) {
        Assert.assertEquals(this.c, arg1);
    }

}
