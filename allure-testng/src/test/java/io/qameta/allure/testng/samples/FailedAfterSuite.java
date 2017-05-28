package io.qameta.allure.testng.samples;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class FailedAfterSuite {

    @AfterSuite
    public void afterSuite() throws Exception {
        throw new RuntimeException();
    }

    @Test
    public void passed() throws Exception {
    }
}
