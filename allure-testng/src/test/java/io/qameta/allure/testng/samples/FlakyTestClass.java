package io.qameta.allure.testng.samples;

import io.qameta.allure.Flaky;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
@Flaky
public class FlakyTestClass {

    @Test
    @Flaky
    public void flakyTest() throws Exception {
    }

    @Test
    public void flakyAsWell() throws Exception {
    }
}
