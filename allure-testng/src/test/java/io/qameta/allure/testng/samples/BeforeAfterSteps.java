package io.qameta.allure.testng.samples;

import io.qameta.allure.Allure;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class BeforeAfterSteps {

    @BeforeMethod
    public void setUp() {
        Allure.step("step in before method");
    }

    @AfterMethod
    public void tearDown() {
        Allure.step("step in after method");
    }

    @Test
    public void sample() {
    }


}
