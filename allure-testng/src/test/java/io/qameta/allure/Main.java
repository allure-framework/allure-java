package io.qameta.allure;

import org.testng.TestNG;

import java.util.Collections;

/**
 * @author charlie (Dmitry Baev).
 */
public class Main {

    public static void main(String[] args) {
        System.setProperty("allure.results.indentOutput", "true");
        System.setProperty("allure.results.directory", "build/allure-results");
        System.setProperty("org.slf4j.simpleLogger.log.io.qameta.allure.testng.AllureTestNg", "info");
        TestNG testNG = new TestNG(false);
        testNG.setTestSuites(Collections.singletonList("src/test/resources/testng2.xml"));
        testNG.run();
    }
}
