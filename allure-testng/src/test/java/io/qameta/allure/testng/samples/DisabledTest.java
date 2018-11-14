package io.qameta.allure.testng.samples;

import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class DisabledTest {

    @Test(enabled = false)
    public void disabled() {
    }
}
