package io.qameta.allure.testng.samples;

import io.qameta.allure.Step;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class BeforeMethods {

//    @BeforeSuite
//    public void beforeSuite() {
//    }
//
//    @AfterSuite
//    public void afterSuite() {
//    }

    @BeforeTest
    public void beforeTest() {
    }

    @AfterTest
    public void afterTest() {
    }

    @BeforeClass
    public void beforeClass() {
    }

    @AfterClass
    public void afterClass() {
    }

    @BeforeMethod(alwaysRun = true)
    public void beforeMethod1() {
    }

    @Step
    @BeforeMethod(alwaysRun = true)
    public void beforeMethod2() {
    }

    @Test
    public void test1() {
    }

    @Test
    public void test2() {
    }

    @Test
    public void test3() {
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod1() {
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod2() {
    }

}
