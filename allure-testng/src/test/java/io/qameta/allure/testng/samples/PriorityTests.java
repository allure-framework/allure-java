package io.qameta.allure.testng.samples;

import org.testng.annotations.Test;

public class PriorityTests {

    @Test
    public void wTest() {
    }
    @Test(priority = 3)
    public void xTest() {
    }

    @Test(priority = 2)
    public void yTest() {
    }

    @Test(priority = 1)
    public void zTest() {
    }

}
