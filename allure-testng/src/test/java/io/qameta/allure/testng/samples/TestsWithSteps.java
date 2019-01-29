package io.qameta.allure.testng.samples;

import org.testng.SkipException;
import org.testng.annotations.Test;

import static io.qameta.allure.Allure.step;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class TestsWithSteps {

    @Test
    public void testWithOneStep() {
        step("Sample step one");
    }

    @Test
    public void failingByAssertion() {
        step("Sample step one");
        step("Failing step", () -> {
            assertThat(2).isEqualTo(1);
        });
    }

    @Test
    public void skipped() {
        step("Sample step one");
        step("skipThisTest", () -> {
            throw new SkipException("Skipped");
        });
    }

    @Test
    public void brokenTest() {
        step("Sample step one");
        step("broken", () -> {
            throw new RuntimeException("Exception");
        });
    }

    @Test
    public void brokenTestWithoutMessage() {
        step("Sample step one");
        step("brokenWithoutMessage", () -> {
            throw new RuntimeException();
        });
    }
}
