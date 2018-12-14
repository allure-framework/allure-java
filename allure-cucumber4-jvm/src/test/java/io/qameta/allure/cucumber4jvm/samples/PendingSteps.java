package io.qameta.allure.cucumber4jvm.samples;

import cucumber.api.PendingException;
import cucumber.api.java.en.Given;

/**
 * @author charlie (Dmitry Baev).
 */
public class PendingSteps {

    @Given("^step is yet to be implemented$")
    public void stepIsYetToBeImplemented() throws Throwable {
        // Write code here that turns the phrase above into concrete actions
        throw new PendingException();
    }

}
