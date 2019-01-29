package io.qameta.allure.testng.samples;

import org.testng.annotations.Test;

import static io.qameta.allure.Allure.step;

/**
 * Author: Sergey Potanin
 * Date: 31/01/2018
 */
public class TestWithTimeout {

    @Test(timeOut = 100)
    public void testWithTimeout() {
        step("Step of the test with timeout");
    }

    @Test(timeOut = 0)
    public void testWithoutTimeout() {
        step("Step of the test with no timeout");
    }

}
