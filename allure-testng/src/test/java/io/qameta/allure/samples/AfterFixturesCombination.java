package io.qameta.allure.samples;

import io.qameta.allure.Step;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class AfterFixturesCombination {
    
    @AfterSuite
    public void afterSuiteTwo() {
        afterSuiteTwoStep();
    }

    @AfterTest
    public void afterTest() {
        stepTwo();
    }

    @AfterMethod
    public void afterMethod() {
        stepThree();
    }

    @Test
    public void test() {

    }

    @Step
    public void afterSuiteTwoStep() {

    }

    @Step
    public void stepTwo() {

    }

    @Step
    public void stepThree() {

    }

}
