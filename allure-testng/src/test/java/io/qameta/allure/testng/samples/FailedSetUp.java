package io.qameta.allure.testng.samples;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static io.qameta.allure.Allure.step;

/**
 * @author charlie (Dmitry Baev).
 */
public class FailedSetUp {

    @BeforeMethod
    public void setUp() {
        throw new RuntimeException("Hey");
    }

    @Test
    public void skippedTest() {
    }

    @AfterMethod(alwaysRun = true)
    public void afterAlways() {
        step("first");
        step("second");
    }
}
