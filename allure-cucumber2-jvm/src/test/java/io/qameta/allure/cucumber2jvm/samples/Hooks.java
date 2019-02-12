package io.qameta.allure.cucumber2jvm.samples;

import cucumber.api.java.After;
import cucumber.api.java.Before;

public class Hooks {

    @Before("@bp")
    public void beforePassed() {
    }

    @Before("@bf")
    public void beforeFailed() {
        throw new AssertionError("This hook should fail");
    }

    @After("@ap")
    public void afterPassed() {
    }

    @After("@af")
    public void afterFailed() {
        throw new AssertionError("This hook should fail");
    }
}
