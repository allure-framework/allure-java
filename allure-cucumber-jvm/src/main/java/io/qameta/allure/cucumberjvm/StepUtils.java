package io.qameta.allure.cucumberjvm;

import cucumber.runtime.CucumberException;
import cucumber.runtime.StepDefinitionMatch;
import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.Step;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.util.ResultsUtils;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * Step utils.
 */
class StepUtils {
    private static final Logger LOG = LoggerFactory.getLogger(StepUtils.class);
    private static final String FAILED = "failed";

    private final AllureLifecycle lifecycle;
    private final Feature feature;
    private final Scenario scenario;

    StepUtils(final Feature feature, final Scenario scenario) {
        this.lifecycle = Allure.getLifecycle();
        this.feature = feature;
        this.scenario = scenario;
    }

    protected Step extractStep(final StepDefinitionMatch match) {
        try {
            final Field step = match.getClass().getDeclaredField("step");
            step.setAccessible(true);
            return (Step) step.get(match);
        } catch (ReflectiveOperationException e) {
            //shouldn't ever happen
            LOG.error(e.getMessage(), e);
            throw new CucumberException(e);
        }
    }

    protected boolean isEqualSteps(final Step step, final Step gherkinStep) {
        return Objects.equals(step.getLine(), gherkinStep.getLine());
    }

    protected void fireCanceledStep(final Step unimplementedStep) {
        final StepResult stepResult = new StepResult();
        stepResult.withName(unimplementedStep.getName())
                .withStart(System.currentTimeMillis())
                .withStop(System.currentTimeMillis())
                .withStatus(Status.SKIPPED)
                .withStatusDetails(new StatusDetails().withMessage("Unimplemented step"));
        lifecycle.startStep(scenario.getId(), getStepUuid(unimplementedStep), stepResult);
        lifecycle.stopStep(getStepUuid(unimplementedStep));

        final StatusDetails statusDetails = new StatusDetails();
        final TagParser tagParser = new TagParser(feature, scenario);
        statusDetails
                .withFlaky(tagParser.isFlaky())
                .withMuted(tagParser.isMuted())
                .withKnown(tagParser.isKnown());
        lifecycle.updateTestCase(scenario.getId(), scenarioResult ->
                scenarioResult.withStatus(Status.SKIPPED)
                        .withStatusDetails(statusDetails
                                .withMessage("Unimplemented steps were found")));
    }

    protected String getStepUuid(final Step step) {
        return feature.getId() + scenario.getId() + step.getName() + step.getLine();
    }

    protected static String getHistoryId(final String id) {
        return Utils.md5(id);
    }

    protected void fireFixtureStep(final Match match, final Result result, final boolean isBefore) {
        final String uuid = Utils.md5(match.getLocation());
        final StepResult stepResult = new StepResult()
                .withName(match.getLocation())
                .withStatus(Status.fromValue(result.getStatus()))
                .withStart(System.currentTimeMillis() - result.getDuration())
                .withStop(System.currentTimeMillis());
        if (FAILED.equals(result.getStatus())) {
            final StatusDetails statusDetails = ResultsUtils.getStatusDetails(result.getError()).get();
            stepResult.withStatusDetails(statusDetails);
            if (isBefore) {
                final TagParser tagParser = new TagParser(feature, scenario);
                statusDetails
                        .withMessage("Before is failed: " + result.getError().getLocalizedMessage())
                        .withFlaky(tagParser.isFlaky())
                        .withMuted(tagParser.isMuted())
                        .withKnown(tagParser.isKnown());
                lifecycle.updateTestCase(scenario.getId(), scenarioResult ->
                        scenarioResult.withStatus(Status.SKIPPED)
                                .withStatusDetails(statusDetails));
            }
        }
        lifecycle.startStep(scenario.getId(), uuid, stepResult);
        lifecycle.stopStep(uuid);
    }
}
