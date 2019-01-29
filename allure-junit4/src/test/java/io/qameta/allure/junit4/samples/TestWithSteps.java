package io.qameta.allure.junit4.samples;

import io.qameta.allure.Allure;
import org.junit.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class TestWithSteps {

    @Test
    public void testWithSteps() {
        Allure.step("step1");
        Allure.step("step2");
        Allure.step("step3");
    }

}
