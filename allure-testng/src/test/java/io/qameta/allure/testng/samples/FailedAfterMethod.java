package io.qameta.allure.testng.samples;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class FailedAfterMethod {

    @AfterMethod
    public void afterMethod() throws Exception {
        throw new RuntimeException();
    }

    @Test
    public void passed() throws Exception {
    }
}
