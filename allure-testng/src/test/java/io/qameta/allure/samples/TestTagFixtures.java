package io.qameta.allure.samples;

import io.qameta.allure.Step;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class TestTagFixtures {

    @BeforeTest
    public void beforeTest1() {
        step();
    }

    @BeforeTest
    public void beforeTest2() {
        step();
    }

    @Test
    public void test() {
        step();
    }

    @AfterTest
    public void afterTest1() {
        step();
    }

    @AfterTest
    public void afterTest2() {
        step();
    }

    @Step
    public void step() {

    }
}
