package io.qameta.allure.junit4.samples;

import io.qameta.allure.Step;
import org.junit.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class TestWithSteps {

    @Test
    public void testWithSteps() throws Exception {
        step1();
        step2();
        step3();
    }

    @Step
    public void step1() {
    }

    @Step
    public void step2() {
    }

    @Step
    public void step3() {
    }
}
