package io.qameta.allure.cucumber4jvm.samples;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.testng.Assert;

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
