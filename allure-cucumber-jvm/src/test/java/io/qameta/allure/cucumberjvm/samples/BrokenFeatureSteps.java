package io.qameta.allure.cucumberjvm.samples;

import cucumber.api.java.en.Given;

/**
 * @author charlie (Dmitry Baev).
 */
public class BrokenFeatureSteps {

    @Given("^everything is broken$")
    public void everythingIsBroken() {
        throw new RuntimeException("Oops");
    }

}
