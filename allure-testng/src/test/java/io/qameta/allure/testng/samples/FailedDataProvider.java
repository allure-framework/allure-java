package io.qameta.allure.testng.samples;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class FailedDataProvider {

    @DataProvider
    public Object[][] dataProvider() {
        throw new RuntimeException("Data provider failed");
    }

    @Test(dataProvider = "dataProvider")
    public void test(String s) {
    }
}
