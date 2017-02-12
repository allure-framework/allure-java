package io.qameta.allure;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ehborisov
 */
public class SampleListener implements TestExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(SampleListener.class);

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        LOGGER.info("on testPlanExecutionStarted {}", testPlan);
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        LOGGER.info("on testPlanExecutionFinished {}", testPlan);
    }

    @Override
    public void dynamicTestRegistered(TestIdentifier testIdentifier) {
        LOGGER.info("on dynamicTestRegistered {}", testIdentifier);
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        LOGGER.info("on executionSkipped {}, {}", testIdentifier, reason);
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        LOGGER.info("on executionStarted {}", testIdentifier);
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
       LOGGER.info("on executionFinished {}, {}", testIdentifier, testExecutionResult);
    }

    @Override
    public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
        LOGGER.info("on reportingEntryPublished {}, {}", testIdentifier, entry);
    }
}
