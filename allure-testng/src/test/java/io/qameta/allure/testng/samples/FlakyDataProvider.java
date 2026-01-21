package io.qameta.allure.testng.samples;

import org.testng.IDataProviderMethod;
import org.testng.IRetryDataProvider;
import org.testng.ITestResult;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class FlakyDataProvider {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    @BeforeClass
    public void reset() {
        COUNTER.set(0);
    }

    public static class Retry implements IRetryDataProvider {
        @Override
        public boolean retry(IDataProviderMethod method) {
            return COUNTER.incrementAndGet() < 2;
        }
    }

    @DataProvider(retryUsing = Retry.class)
    public Object[][] provide() {
        if (COUNTER.get() == 0) {
            throw new RuntimeException("Simulated DataProvider Failure");
        }
        return new Object[][]{{"data"}};
    }

    @Test(dataProvider = "provide")
    public void test(String s) {
    }
}
