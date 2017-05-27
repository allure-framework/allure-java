package io.qameta.allure.testng.samples;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class FailedBeforeMethod {

    @BeforeMethod
    public void beforeMethod() throws Exception {
        throw new RuntimeException();
    }

    @Test
    public void skipped() throws Exception {
    }
}
