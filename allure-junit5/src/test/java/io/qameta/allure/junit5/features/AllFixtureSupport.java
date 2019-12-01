package io.qameta.allure.junit5.features;

import io.qameta.allure.Allure;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllFixtureSupport {

    @BeforeAll
    static void setUpAll() {
        Allure.step("setUpAll 1");
        Allure.step("setUpAll 2");
    }

    @Test
    void test1() {
        Allure.step("test1 1");
        Allure.step("test1 2");
    }

    @AfterAll
    static void tearDownAll() {
        Allure.step("tearDownAll 1");
        Allure.step("tearDownAll 2");
    }
}
