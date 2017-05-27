package io.qameta.allure.testng.samples;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class FailedTestPassedBeforeFixture {

    @BeforeTest
    public void beforeTestPassed() throws Exception {
    }

    @Test
    public void broken() throws Exception {
        throw new RuntimeException();
    }
}
