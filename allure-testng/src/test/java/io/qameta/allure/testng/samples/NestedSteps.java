package io.qameta.allure.testng.samples;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import static io.qameta.allure.Allure.step;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class NestedSteps {

    @BeforeSuite
    public void beforeSuite() {
        step("stepOne", () -> step("nestedStep"));
    }

    @BeforeMethod
    public void beforeMethod() {
        step("stepTwo", () -> step("nestedStep"));
    }

    @Test
    public void test() {
        step("stepThree", () -> step("nestedStep"));
    }
}
