package io.qameta.allure.cucumber4jvm.samples;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author hamzaan.bridle
 */
public class RetriesSteps {

    public static AtomicInteger aFlakyTestRunCount = new AtomicInteger(0);
    public static AtomicInteger brokenBeforeRunCount = new AtomicInteger(0);
    public static AtomicInteger aFlakyGivenRunCount = new AtomicInteger(0);

    @Before("@RetrySkipped")
    public void brokenBefore() {
        final int timesRun = brokenBeforeRunCount.incrementAndGet();
        if (timesRun < 3) {
            throw new RuntimeException("Test retries don't change skip behaviour when before is broken");
        }
    }

    @Given("^a flaky test$")
    public void aFlakyTest() {
        // nothing here
    }

    @Given("^a flaky given")
    public void aFlakyGiven() {
        aFlakyGivenRunCount.incrementAndGet();
        assertThat(aFlakyGivenRunCount).hasValue(3);
    }

    @When("^the test is executed$")
    public void testIsExecuted() {
        final int timesRun = aFlakyTestRunCount.incrementAndGet();
        if (timesRun < 3) {
            throw new RuntimeException("Will fail before third run");
        }
    }

    @Then("^the test only passes on the third run.")
    public void testOnlyPassesOnThirdRun() {
        // nothing here
    }

}
