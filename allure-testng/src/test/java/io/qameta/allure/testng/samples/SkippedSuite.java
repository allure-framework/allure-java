package io.qameta.allure.testng.samples;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import static io.qameta.allure.Allure.step;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class SkippedSuite {

    @BeforeSuite
    public void skipSuite() {
        step("failingStep", () -> {
            throw new RuntimeException("Skip all");
        });
    }

    @BeforeMethod
    public void skippedBeforeMethod() {

    }

    @Test
    public void skippedTest() {

    }
}
