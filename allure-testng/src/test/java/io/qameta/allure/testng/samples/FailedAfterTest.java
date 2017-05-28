package io.qameta.allure.testng.samples;

import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class FailedAfterTest {

    @AfterTest
    public void afterTest() throws Exception {
        throw new RuntimeException();
    }

    @Test
    public void passed() throws Exception {
    }
}
