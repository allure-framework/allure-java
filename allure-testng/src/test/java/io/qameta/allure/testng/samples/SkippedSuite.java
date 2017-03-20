package io.qameta.allure.testng.samples;

import io.qameta.allure.Step;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class SkippedSuite {

    @BeforeSuite
    public void skipSuite(){
        failingStep();
    }

    @BeforeMethod
    public void skippedBeforeMethod(){

    }

    @Step
    private void failingStep(){
        throw new RuntimeException("Skip all");
    }

    @Test
    public void skippedTest(){

    }
}
