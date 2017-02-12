package io.qameta.allure;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class MainTest {

    @BeforeSuite
    public void loadTestConfiguration() throws Exception {
    }

    @Test
    public void shouldMainTest() throws Exception {
        myStep("fast", "and cool");
    }

    @Step
    public void myStep(String firstParam, String secondParam) {
    }
}
