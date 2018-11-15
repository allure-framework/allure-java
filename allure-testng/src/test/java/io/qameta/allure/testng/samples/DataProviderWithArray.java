package io.qameta.allure.testng.samples;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class DataProviderWithArray {

    @DataProvider(name = "someProvider")
    public static Object[][] someProvider() {
        return new Object[][]{
                new Object[]{"a", false, new int[]{1, 2, 3}}
        };
    }

    @Test(dataProvider = "someProvider")
    public void someTest(final String first, final boolean second, final int[] third) {
    }

}
