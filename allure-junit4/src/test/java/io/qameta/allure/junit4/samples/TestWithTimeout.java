package io.qameta.allure.junit4.samples;

import io.qameta.allure.Step;
import org.junit.Test;

/**
 * Author: Sergey Potanin
 * Date: 02/02/2018
 */
public class TestWithTimeout {

    @Test(timeout = 100)
    public void testWithSteps() {
        step1();
        step2();
    }

    @Step("Step 1")
    private void step1() {
    }

    @Step("Step 2")
    private void step2() {
    }

}
