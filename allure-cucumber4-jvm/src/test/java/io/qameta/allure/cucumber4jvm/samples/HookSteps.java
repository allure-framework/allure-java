package io.qameta.allure.cucumber4jvm.samples;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import org.testng.Assert;

/**
 * @author letsrokk (Dmitry Mayer).
 */
public class HookSteps {

    @Before("@WithHooks")
    public void beforeHook(){
        // nothing
    }

    @After("@WithHooks")
    public void afterHook(){
        // nothing
    }

    @Before("@BeforeHookWithException")
    public void beforeHookWithException(){
        Assert.fail("Exception in Hook step");
    }

    @After("@AfterHookWithException")
    public void afterHookWithException(){
        Assert.fail("Exception in Hook step");
    }
}
