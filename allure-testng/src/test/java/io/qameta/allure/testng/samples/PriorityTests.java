package io.qameta.allure.testng.samples;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static io.qameta.allure.Allure.parameter;

public class PriorityTests {

    private final static String ORDER_PARAMETER = "order";

    private final AtomicInteger cnt = new AtomicInteger();

    @DataProvider(name = "someProvider")
    public static Object[][] someProvider() {
        return new Object[][]{
                new Object[]{1},
                new Object[]{2}
        };
    }

    @Test(dataProvider = "someProvider", priority = 4)
    public void vTest(int parameter) {
        parameter(ORDER_PARAMETER, cnt.incrementAndGet());
    }

    @Test(priority = 3)
    public void wTest() {
        parameter(ORDER_PARAMETER, cnt.incrementAndGet());
    }

    @Test(priority = 2)
    public void xTest() {
        parameter(ORDER_PARAMETER, cnt.incrementAndGet());
    }

    @Test(priority = 1)
    public void yTest() {
        parameter(ORDER_PARAMETER, cnt.incrementAndGet());
    }

    @Test
    public void zTest() {
        parameter(ORDER_PARAMETER, cnt.incrementAndGet());
    }

}
