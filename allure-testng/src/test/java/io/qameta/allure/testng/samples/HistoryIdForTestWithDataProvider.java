package io.qameta.allure.testng.samples;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class HistoryIdForTestWithDataProvider {

    @DataProvider(name = "getData")
    public static Object[][] getData() {
        return new Object[][]{
                new Object[]{"first"},
                new Object[]{"first"},
                new Object[]{"second"}
        };
    }

    @Test(dataProvider = "getData")
    public void simpleTest(String param) throws Exception {
    }
}
