package io.qameta.allure.testng.samples;

import io.qameta.allure.Step;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class PerMethodFixtures {

    @BeforeMethod
    public void beforeMethod1() {
        step();
    }

    @BeforeMethod
    public void beforeMethod2() {
        step();
    }

    @Test
    public void test1() {
        step();
    }

    @Test
    public void test2() {
        step();
    }

    @AfterMethod
    public void afterMethod1() {
        step();
    }

    @AfterMethod
    public void afterMethod2() {
        step();
    }

    @Step
    public void step() {

    }
}
