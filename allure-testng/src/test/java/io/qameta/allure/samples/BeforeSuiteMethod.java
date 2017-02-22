package io.qameta.allure.samples;

import io.qameta.allure.Step;
import org.testng.annotations.BeforeSuite;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class BeforeSuiteMethod {

    @BeforeSuite
    public void beforeSuiteOne() {
        beforeSuiteOneStep();
    }

    @Step
    private void beforeSuiteOneStep() {

    }
}
