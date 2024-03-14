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
package io.qameta.allure.cucumber5jvm.samples;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.qameta.allure.Allure;

/**
 * @author charlie (Dmitry Baev).
 */
public class RuntimeApiSteps {

    @Before("@beforeScenario")
    public void beforeScenario(){
        // nothing
    }

    @Before("@beforeFeature")
    public void beforeFeature(){
        // nothing
    }

    @When("^step 1$")
    public void step1() {
        Allure.step("step1 nested");
        Allure.link("step1", "https://example.org/step1");
        Allure.getLifecycle().getCurrentTestCase().ifPresent(uuid -> {
            System.out.println("step1: " + uuid);
        });
    }

    @When("^step 2$")
    public void step2() {
        Allure.step("step2 nested");
        Allure.link("step2", "https://example.org/step2");
        Allure.getLifecycle().getCurrentTestCase().ifPresent(uuid -> {
            System.out.println("step2: " + uuid);
        });
    }

    @And("^step 3$")
    public void step3() {
        Allure.step("step3 nested");
        Allure.link("step3", "https://example.org/step3");
        Allure.getLifecycle().getCurrentTestCase().ifPresent(uuid -> {
            System.out.println("step3: " + uuid);
        });
    }

    @Then("^step 4$")
    public void step4() {
        Allure.step("step4 nested");
        Allure.link("step4", "https://example.org/step4");
        Allure.getLifecycle().getCurrentTestCase().ifPresent(uuid -> {
            System.out.println("step4: " + uuid);
        });
    }

    @And("^step 5$")
    public void step5() {
        Allure.step("step5 nested");
        Allure.link("step5", "https://example.org/step5");
        Allure.getLifecycle().getCurrentTestCase().ifPresent(uuid -> {
            System.out.println("step5: " + uuid);
        });
    }
}
