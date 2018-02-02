package io.qameta.allure.testng.samples;

import io.qameta.allure.Step;
import org.testng.annotations.Test;

/**
 * Author: Sergey Potanin
 * Date: 31/01/2018
 */
public class TestWithTimeout {

    @Test(timeOut = 100)
    public void testWithTimeout() {
        stepForTimeout();
    }

    @Test(timeOut = 0)
    public void testWithoutTimeout() {
        step();
    }

    @Step("Step of the test with timeout")
    private void stepForTimeout() {
    }

    @Step("Step of the test with no timeout")
    private void step() {
    }

}
