package io.qameta.allure.testng.samples;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class FailedBeforeSuite {

    @BeforeSuite
    public void beforeSuite() throws Exception {
        throw new RuntimeException();
    }

    @Test
    public void skipped() throws Exception {
    }
}
