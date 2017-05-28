package io.qameta.allure.testng.samples;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class FailedBeforeTest {

    @BeforeTest
    public void beforeTest() throws Exception {
        throw new RuntimeException();
    }

    @Test
    public void skipped() throws Exception {
    }
}
