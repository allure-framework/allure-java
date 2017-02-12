package io.qameta.allure;

import io.qameta.allure.testng.AllureTestNg;
import org.testng.ITestNGListener;
import org.testng.TestNG;

import java.util.Collections;

/**
 * @author charlie (Dmitry Baev).
 */
public class Main {

    public static void main(String[] args) {
        System.setProperty("allure.results.indentOutput", "true");
        System.setProperty("org.slf4j.simpleLogger.log.io.qameta.allure.AllureTestNg", "info");
        TestNG testNG = new TestNG(false);
        testNG.addListener((ITestNGListener) new AllureTestNg());
        testNG.setTestSuites(Collections.singletonList("src/test/resources/testng2.xml"));
        testNG.run();
    }
}
