package io.qameta.allure.testng.samples;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class FixtureWithDescription {

    @BeforeMethod(description = "Set up method with description")
    public void setUp() throws Exception {

    }

    @Test
    public void someTest() throws Exception {
    }
}
