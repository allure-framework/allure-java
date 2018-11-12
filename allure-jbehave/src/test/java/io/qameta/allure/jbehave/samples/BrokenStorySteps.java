package io.qameta.allure.jbehave.samples;

import org.jbehave.core.annotations.Given;

/**
 * @author charlie (Dmitry Baev).
 */
public class BrokenStorySteps {

    @Given("everything is broken")
    public void everythingIsBroken() {
        throw new RuntimeException("Oops");
    }

}
