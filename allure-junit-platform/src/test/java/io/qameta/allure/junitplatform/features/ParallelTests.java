package io.qameta.allure.junitplatform.features;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.concurrent.TimeUnit;

/**
 * @author charlie (Dmitry Baev).
 */
@Execution(ExecutionMode.CONCURRENT)
public class ParallelTests {

    @Test
    void first() throws InterruptedException {
        TimeUnit.SECONDS.sleep(1);
    }

    @Test
    void second() throws InterruptedException {
        TimeUnit.SECONDS.sleep(1);
    }
}
