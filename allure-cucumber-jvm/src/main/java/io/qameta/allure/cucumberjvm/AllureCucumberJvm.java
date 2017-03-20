package io.qameta.allure.cucumberjvm;

import cucumber.runtime.CucumberException;
import cucumber.runtime.StepDefinitionMatch;
import gherkin.I18n;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.*;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.ResultsUtils;

import static io.qameta.allure.ResultsUtils.getHostName;
import static io.qameta.allure.ResultsUtils.getThreadName;

import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.math.BigInteger;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Allure plugin for Cucumber-JVM.
 */
public class AllureCucumberJvm implements Reporter, Formatter {

    private static final Logger LOG = LoggerFactory.getLogger(AllureCucumberJvm.class);
    private static final List<String> SCENARIO_OUTLINE_KEYWORDS = Collections.synchronizedList(new ArrayList<String>());

    private static final String FAILED = "failed";
    private static final String PASSED = "passed";
    private static final String SKIPPED = "skipped";

    private static final String MD_5 = "md5";
    private static final String COMPOSITE_TAG_DELIMITER = "=";

    private static final String FLAKY = "@FLAKY";
    private static final String KNOWN = "@KNOWN";
    private static final String MUTED = "@MUTED";
    private static final String SEVERITY = "@SEVERITY";

    private static final String ISSUE_LINK = "@ISSUE";
    private static final String LINK = "@LINK";
    private static final String TMS_LINK = "@TMSLINK";

    private final LinkedList<Step> gherkinSteps = new LinkedList<>();
    private final AllureLifecycle lifecycle;
    private Feature feature;
    private StepDefinitionMatch match;
    private Scenario scenario;


    public AllureCucumberJvm() {
        this.lifecycle = Allure.getLifecycle();
        final List<I18n> i18nList = I18n.getAll();

        i18nList.forEach((i18n) -> SCENARIO_OUTLINE_KEYWORDS.addAll(i18n.keywords("scenario_outline")));
    }

    @Override
    public void feature(Feature feature) {
        this.feature = feature;
    }

    @Override
    public void before(Match match, Result result) {
        fireFixtureStep(match, result, true);
    }

    @Override
    public void after(Match match, Result result) {
        fireFixtureStep(match, result, false);
    }

    @Override
    public void startOfScenarioLifeCycle(Scenario scenario) {
        this.scenario = scenario;
        final List<Label> scenarioLabels = new ArrayList<>();
        final List<Link> scenarioLinks = new ArrayList<>();

        final LinkedList<Tag> tags = new LinkedList<>();
        tags.addAll(scenario.getTags());

        if (SCENARIO_OUTLINE_KEYWORDS.contains(scenario.getKeyword())) {
            synchronized (gherkinSteps) {
                gherkinSteps.clear();
            }
        } else {
            tags.addAll(feature.getTags());
        }

        scenarioLabels.add(getFeatureLabel(feature.getName()));
        scenarioLabels.add(getStoryLabel(scenario.getName()));

        while (tags.peek() != null) {
            final Tag tag = tags.remove();

            final String tagString = tag.getName().toUpperCase();

            if (tagString.contains(COMPOSITE_TAG_DELIMITER)) {

                final String tagKey = tagString.split(COMPOSITE_TAG_DELIMITER)[0];
                final String tagValue = tagString.split(COMPOSITE_TAG_DELIMITER)[1];

                switch (tagKey) {
                    case SEVERITY:
                        try {
                            scenarioLabels.add(getSeverityLabel(tagValue));
                        } catch (IllegalArgumentException e) {
                            LOG.warn("There is no severity level {} failing back to 'normal'", tagValue);
                        }
                        break;
                    case TMS_LINK:
                        scenarioLinks.add(ResultsUtils.createTmsLink(tagValue));
                        break;
                    case ISSUE_LINK:
                        scenarioLinks.add(ResultsUtils.createIssueLink(tagValue));
                        break;
                    default:
                        LOG.warn("Composite tag {} is not supported. adding it as RAW", tagKey);
                        scenarioLabels.add(getTagLabel(tag));
                        break;
                }
            } else if (!isResultTag(tag)) {
                scenarioLabels.add(getTagLabel(tag));
            }
        }

        scenarioLabels.add(new Label().withName("host").withValue(getHostName()));
        scenarioLabels.add(new Label().withName("package").withValue(feature.getName()));
        scenarioLabels.add(new Label().withName("suite").withValue(feature.getName()));
        scenarioLabels.add(new Label().withName("testClass").withValue(scenario.getName()));
        scenarioLabels.add(new Label().withName("thread").withValue(getThreadName()));

        final TestResult result = new TestResult()
                .withUuid(scenario.getId())
                .withHistoryId(getHistoryId(scenario.getId()))
                .withName(scenario.getName())
                .withLabels(scenarioLabels)
                .withLinks(scenarioLinks);

        if (!feature.getDescription().isEmpty()) {
            result.withDescription(feature.getDescription());
        }

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(scenario.getId());

    }

    @Override
    public void step(Step step) {
        synchronized (gherkinSteps) {
            gherkinSteps.add(step);
        }
    }

    @Override
    public void match(Match match) {
        if (match instanceof StepDefinitionMatch) {
            this.match = (StepDefinitionMatch) match;
            final Step step = extractStep(this.match);
            synchronized (gherkinSteps) {
                while (gherkinSteps.peek() != null && !isEqualSteps(step, gherkinSteps.peek())) {
                    fireCanceledStep(gherkinSteps.remove());
                }
                if (isEqualSteps(step, gherkinSteps.peek())) {
                    gherkinSteps.remove();
                }
            }
            final StepResult stepResult = new StepResult();
            stepResult.withName(String.format("%s %s", step.getKeyword(), step.getName()))
                    .withStart(System.currentTimeMillis());
            lifecycle.startStep(scenario.getId(), getStepUuid(step), stepResult);
        }
    }

    @Override
    public void result(Result result) {
        if (match != null) {
            final StatusDetails statusDetails = new StatusDetails();
            statusDetails
                    .withFlaky(isFlaky(scenario))
                    .withMuted(isMuted(scenario))
                    .withKnown(isKnown(scenario));

            switch (result.getStatus()) {
                case FAILED:
                    lifecycle.updateStep(stepResult -> stepResult.withStatus(Status.FAILED));
                    lifecycle.updateTestCase(scenario.getId(), scenarioResult ->
                            scenarioResult.withStatus(Status.FAILED)
                                    .withStatusDetails(statusDetails
                                            .withMessage(result.getError().getLocalizedMessage())
                                            .withTrace(getStackTraceAsString(result.getError()))
                                    ));
                    lifecycle.stopStep();
                    break;
                case SKIPPED:
                    lifecycle.updateStep(stepResult -> stepResult.withStatus(Status.SKIPPED));
                    lifecycle.stopStep();
                    break;
                case PASSED:
                    lifecycle.updateStep(stepResult -> stepResult.withStatus(Status.PASSED));
                    lifecycle.stopStep();
                    lifecycle.updateTestCase(scenario.getId(), scenarioResult ->
                            scenarioResult.withStatus(Status.PASSED)
                                    .withStatusDetails(statusDetails));
                    break;
                default:
                    break;
            }
            match = null;
        }
    }

    @Override
    public void endOfScenarioLifeCycle(Scenario scenario) {
        synchronized (gherkinSteps) {
            while (gherkinSteps.peek() != null) {
                fireCanceledStep(gherkinSteps.remove());
            }
        }
        lifecycle.stopTestCase(scenario.getId());
        lifecycle.writeTestCase(scenario.getId());
    }

    @Override
    public void embedding(String string, byte[] bytes) {
        //Nothing to do with Allure
    }

    @Override
    public void write(String string) {
        //Nothing to do with Allure
    }

    @Override
    public void syntaxError(String state, String event, List<String> legalEvents, String uri, Integer line) {
        //Nothing to do with Allure
    }

    @Override
    public void uri(String uri) {
        //Nothing to do with Allure
    }

    @Override
    public void scenarioOutline(ScenarioOutline so) {
        //Nothing to do with Allure
    }

    @Override
    public void examples(Examples exmpls) {
        //Nothing to do with Allure
    }


    @Override
    public void background(Background b) {
        //Nothing to do with Allure
    }

    @Override
    public void scenario(Scenario scnr) {
        //Nothing to do with Allure
    }

    @Override
    public void done() {
        //Nothing to do with Allure
    }

    @Override
    public void close() {
        //Nothing to do with Allure
    }

    @Override
    public void eof() {
        //Nothing to do with Allure

    }

    private Step extractStep(StepDefinitionMatch match) {
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

    private boolean isEqualSteps(Step step, Step gherkinStep) {
        return Objects.equals(step.getLine(), gherkinStep.getLine());
    }

    private void fireCanceledStep(Step unimplementedStep) {
        final StepResult stepResult = new StepResult();
        stepResult.withName(unimplementedStep.getName())
                .withStart(System.currentTimeMillis())
                .withStop(System.currentTimeMillis())
                .withStatus(Status.SKIPPED)
                .withStatusDetails(new StatusDetails().withMessage("Unimplemented step"));
        lifecycle.startStep(this.scenario.getId(), getStepUuid(unimplementedStep), stepResult);
        lifecycle.stopStep(getStepUuid(unimplementedStep));

        final StatusDetails statusDetails = new StatusDetails();
        statusDetails
                .withFlaky(isFlaky(scenario))
                .withMuted(isMuted(scenario))
                .withKnown(isKnown(scenario));
        lifecycle.updateTestCase(scenario.getId(), scenarioResult ->
                scenarioResult.withStatus(Status.SKIPPED)
                        .withStatusDetails(statusDetails
                                .withMessage("Unimplemented steps were found")));
    }

    private String getStepUuid(Step step) {
        return feature.getId() + scenario.getId() + step.getName() + step.getLine();
    }

    private String getHistoryId(final String id) {
        return md5(id);
    }

    private String md5(final String source) {
        final byte[] bytes = getMessageDigest().digest(source.getBytes(UTF_8));
        return new BigInteger(1, bytes).toString(16);
    }

    private MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance(MD_5);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find md5 hashing algorithm", e);
        }
    }

    private static String getStackTraceAsString(Throwable throwable) {
        final StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    private Label getFeatureLabel(String featureName) {

        return ResultsUtils.createLabel(new io.qameta.allure.Feature() {
            @Override
            public String value() {
                return featureName;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return io.qameta.allure.Feature.class;
            }
        });
    }

    private Label getStoryLabel(String storyName) {
        return ResultsUtils.createLabel(new Story() {
            @Override
            public String value() {
                return storyName;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Story.class;
            }
        });
    }

    private Label getSeverityLabel(String severity) {
        return ResultsUtils.createLabel(new Severity() {
            @Override
            public SeverityLevel value() {
                return SeverityLevel.valueOf(severity);
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Severity.class;
            }
        });
    }

    private Label getTagLabel(Tag tag) {
        return new Label().withName("tag").withValue(tag.getName().substring(1));
    }

    private boolean isFlaky(Scenario scenario) {
        return getStatusDetailByTag(scenario, FLAKY);
    }

    private boolean isMuted(Scenario scenario) {
        return getStatusDetailByTag(scenario, MUTED);
    }

    private boolean isKnown(Scenario scenario) {
        return getStatusDetailByTag(scenario, KNOWN);
    }

    private boolean getStatusDetailByTag(Scenario scenario, String tagName) {
        return scenario.getTags().stream()
                .anyMatch(tag -> tag.getName().equalsIgnoreCase(tagName))
                || feature.getTags().stream()
                .anyMatch(tag -> tag.getName().equalsIgnoreCase(tagName));
    }

    private boolean isResultTag(Tag tag) {
        return Arrays.asList(new String[]{FLAKY, KNOWN, MUTED})
                .contains(tag.getName().toUpperCase());
    }

    private void fireFixtureStep(Match match, Result result, boolean isBefore) {
        final String uuid = UUID.randomUUID().toString();
        final StepResult stepResult = new StepResult()
                .withName(match.getLocation())
                .withStatus(Status.fromValue(result.getStatus()))
                .withStart(System.currentTimeMillis() - result.getDuration())
                .withStop(System.currentTimeMillis());
        if (FAILED.equals(result.getStatus())) {
            stepResult.withStatusDetails(new StatusDetails()
                    .withMessage(result.getError().getLocalizedMessage())
                    .withTrace(getStackTraceAsString(result.getError())));
            if (isBefore) {
                final StatusDetails statusDetails = new StatusDetails();
                statusDetails
                        .withFlaky(isFlaky(scenario))
                        .withMuted(isMuted(scenario))
                        .withKnown(isKnown(scenario));
                lifecycle.updateTestCase(scenario.getId(), scenarioResult ->
                        scenarioResult.withStatus(Status.SKIPPED)
                                .withStatusDetails(statusDetails
                                        .withMessage("Before is failed: "
                                                + result.getError().getLocalizedMessage())
                                        .withTrace(getStackTraceAsString(result.getError()))));
            }
        }
        lifecycle.startStep(scenario.getId(), uuid, stepResult);
        lifecycle.stopStep(uuid);
    }
}
