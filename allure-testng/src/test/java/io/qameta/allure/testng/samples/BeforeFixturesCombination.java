package io.qameta.allure.testng.samples;

import io.qameta.allure.Step;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class BeforeFixturesCombination {

    @BeforeSuite
    public void beforeSuiteTwo() {
        beforeSuiteTwoStep();
    }

    @BeforeTest
    public void beforeTest() {
        stepTwo();
    }

    @BeforeMethod
    public void beforeMethod() {
        stepThree();
    }

    @Test
    public void test() {

    }

    @Step
    public void beforeSuiteTwoStep() {

    }

    @Step
    public void stepTwo() {

    }

    @Step
    public void stepThree() {

    }

}
