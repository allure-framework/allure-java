package io.qameta.allure.testng.samples;

import io.qameta.allure.Step;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * eroshenkoam
 * 16.05.17
 */
public class PerClassFixtures {

    @BeforeClass
    public void beforeClass() {
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

    @AfterClass
    public void afterClass() {
        step();
    }

    @Step
    public void step() {

    }

}
