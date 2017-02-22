package io.qameta.allure.samples;

import io.qameta.allure.DisplayName;
import io.qameta.allure.Parameter;
import io.qameta.allure.Step;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * @author ehborisov
 */
public class ParallelMethods {


    @BeforeSuite
    public void beforeSuite() {
        stepOne();
    }

    @BeforeTest
    public void beforeTest() {
        stepTwo();
    }

    @BeforeTest
    public void beforeTest2() {}

    @BeforeSuite
    public void beforeSuite2() throws IOException {
    }

    @BeforeMethod
    public void beforeMethod() {
        stepThree();
    }

    @BeforeMethod
    public void beforeMethod2() {
        stepThree();
    }

    @Test
    public void test1() throws IOException {
        stepFour();
    }

    @DataProvider(name = "dataProvider")
    public Object[][] getTestData() {
        return new Object[][]{
                {"param11", "param12"},
                {"param21", "param22"}
        };
    }

    @Test(dataProvider = "dataProvider")
    @DisplayName("Test 2")
    public void test2(@Parameter("param1") String param1, @Parameter("param2") String param2) throws IOException {
        stepSix();
    }

    @Step("Step one")
    public void stepOne() {

    }

    @Step("Step two")
    public void stepTwo() {

    }

    @Step("Step three")
    public void stepThree() {

    }

    @Step("Step four")
    public void stepFour() {

    }

    @Step("Step five")
    public void stepFive() {

    }

    @Step("Step six")
    public void stepSix() {

    }

    @Step("Step seven")
    public void stepSeven() {

    }

    @Step("Step eight")
    public void stepEight() {

    }

    @Step("Step nine")
    public void stepNine() {

    }

    @AfterSuite
    public void afterSuite() {
        stepSeven();
    }

    @AfterTest
    public void afterTest() {
        stepEight();
    }

    @AfterTest
    public void afterTest2() {
        stepEight();
    }

    @AfterMethod
    public void afterMethod() {
        stepNine();
    }

}
