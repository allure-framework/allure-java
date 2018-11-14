package io.qameta.allure.testng.samples;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class NullParamTest {

    @DataProvider(name = "someProvider")
    public static Object[][] someProvider() {
        return new Object[][]{
                new Object[]{null}
        };
    }

    @Test(dataProvider = "someProvider")
    public void someTest(final String param) {
    }
}
