package io.qameta.allure.samples;

import io.qameta.allure.Flaky;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class FlakyMethods {

    @Test
    @Flaky
    public void flakyTest() throws Exception {
    }

    @Test
    public void notFlaky() throws Exception {
    }
}
