package io.qameta.allure;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class OneBeforeMethod {

    @BeforeMethod
    public void setUp() throws Exception {
        firstStep();
        secondStep();
        thirdStep();
    }

    @Test
    public void shouldHasOneBeforeMethod() throws Exception {
        subStep();
        thirdStep();
        firstStep();
    }

    @Step
    public void firstStep() {
        subStep();
        subStep();
        subStep();
    }

    @Step
    public void secondStep() {
    }

    @Step
    public void thirdStep() {
    }

    @Step
    public void subStep() {
    }

}
