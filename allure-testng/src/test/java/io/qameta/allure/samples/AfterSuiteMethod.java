package io.qameta.allure.samples;

import io.qameta.allure.Step;
import org.testng.annotations.AfterSuite;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class AfterSuiteMethod {

    @AfterSuite
    public void afterSuiteOne(){
        afterSuiteOneStep();
    }

    @Step
    private void afterSuiteOneStep(){

    }
}
