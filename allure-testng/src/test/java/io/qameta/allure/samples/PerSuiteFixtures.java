package io.qameta.allure.samples;

import io.qameta.allure.Step;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class PerSuiteFixtures {

    @BeforeSuite
    public void beforeSuite1() {
        step();
    }

    @BeforeSuite
    public void beforeSuite2() {
        step();
    }

    @Test
    public void test() {
        step();
    }

    @AfterSuite
    public void afterSuite1() {
        step();
    }

    @AfterSuite
    public void afterSuite2() {
        step();
    }

    @Step
    public void step(){

    }
}
