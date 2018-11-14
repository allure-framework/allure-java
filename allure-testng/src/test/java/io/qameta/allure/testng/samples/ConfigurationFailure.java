package io.qameta.allure.testng.samples;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class ConfigurationFailure {

    @BeforeTest(description = "failed configuration")
    public void setUp() {
        throw new RuntimeException("fail");
    }

    @Test
    public void someTest() {
    }

}
