package io.qameta.allure.samples;

import io.qameta.allure.Step;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class NestedSteps {

    @BeforeSuite
    public void beforeSuite(){
        stepOne();
    }

    @BeforeMethod
    public void beforeMethod(){
        stepTwo();
    }

    @Test
    public void test(){
        stepThree();
    }

    @Step
    private void stepOne(){
        nestedStep();
    }

    @Step
    private void nestedStep(){

    }

    @Step
    private void stepTwo(){
        nestedStep();
    }

    @Step
    private void stepThree(){
        nestedStep();
    }
}
