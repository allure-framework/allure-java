package io.qameta.allure.junit4.samples;

import org.junit.Test;

import static io.qameta.allure.Allure.step;

/**
 * Author: Sergey Potanin
 * Date: 02/02/2018
 */
public class TestWithTimeout {

    @Test(timeout = 100)
    public void testWithSteps() {
        step("Step 1");
        step("Step 2");
    }

}
