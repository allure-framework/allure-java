package io.qameta.allure.cucumberjvm;

import cucumber.runtime.CucumberException;
import cucumber.runtime.StepDefinitionMatch;
import gherkin.I18n;
import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.*;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Flaky;
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
import java.util.*;
import java.util.stream.Collectors;

public class AllureCucumberJvm implements Reporter, Formatter {

    private static final Logger LOG = LoggerFactory.getLogger(AllureCucumberJvm.class);
    private static final List<String> SCENARIO_OUTLINE_KEYWORDS = Collections.synchronizedList(new ArrayList<String>());
    private final LinkedList<Step> gherkinSteps = new LinkedList<>();
    private final AllureLifecycle lifecycle;
    private Feature feature;
    private Scenario scenario;
    private StepDefinitionMatch match;

    private static final String FAILED = "failed";
    private static final String SKIPPED = "skipped";
    private static final String MD_5 = "md5";

    private static final String SEVERITY = "@SEVERITY";
    private static final String FLAKY = "@FLAKY";
    private static final String MUTED = "@MUTED";
    private static final String KNOWN = "@KNOWN";

    private static final String ISSUE_LINK = "@ISSUE";
    private static final String TMS_LINK = "@TMSLINK";
    private static final String LINK = "@LINK";

    public AllureCucumberJvm() {
        this.lifecycle = Allure.getLifecycle();
        List<I18n> i18nList = I18n.getAll();

        i18nList.forEach((i18n) -> {
            SCENARIO_OUTLINE_KEYWORDS.addAll(i18n.keywords("scenario_outline"));
        });
    }

    @Override
    public void before(Match match, Result result) {
        //Nothing to do with Allure
    }

    @Override
    public void result(Result result) {
        if (match != null) {
            StatusDetails statusDetails = new StatusDetails();
            statusDetails
                    .withFlaky(isFlaky(scenario))
                    .withMuted(isMuted(scenario))
                    .withKnown(isKnown(scenario));

            if (FAILED.equals(result.getStatus())) {
                lifecycle.updateStep(stepResult -> stepResult.withStatus(Status.FAILED));
                lifecycle.updateTestCase(scenario.getId(), scenarioResult
                        -> scenarioResult.withStatus(Status.FAILED).
                                withStatusDetails(statusDetails
                                        .withMessage(result.getError().getLocalizedMessage())
                                        .withTrace(getStackTraceAsString(result.getError()))
                                ));
                lifecycle.stopStep();
            } else if (SKIPPED.equals(result.getStatus())) {

                lifecycle.updateStep(stepResult -> stepResult.withStatus(Status.SKIPPED));
                lifecycle.stopStep();
                lifecycle.updateTestCase(scenario.getId(), scenarioResult
                        -> scenarioResult.withStatus(Status.SKIPPED)
                                .withStatusDetails(statusDetails
                                        .withMessage("Unimplemented steps were found")));
            } else {
                lifecycle.updateStep(stepResult -> stepResult.withStatus(Status.PASSED));
                lifecycle.stopStep();
                lifecycle.updateTestCase(scenario.getId(), scenarioResult
                        -> scenarioResult.withStatus(Status.PASSED)
                                .withStatusDetails(statusDetails));
            }
            match = null;
        }
    }

    @Override
    public void after(Match match, Result result) {
        //Nothing to do with Allure
    }

    @Override
    public void match(Match match) {
        if (match instanceof StepDefinitionMatch) {
            this.match = (StepDefinitionMatch) match;
            Step step = extractStep(this.match);
            synchronized (gherkinSteps) {
                while (gherkinSteps.peek() != null && !isEqualSteps(step, gherkinSteps.peek())) {
                    fireCanceledStep(gherkinSteps.remove());
                }
                if (isEqualSteps(step, gherkinSteps.peek())) {
                    gherkinSteps.remove();
                }
            }
            StepResult stepResult = new StepResult();
            stepResult.withName(String.format("%s %s", step.getKeyword(), step.getName()))
                    .withStart(System.currentTimeMillis());
            lifecycle.startStep(this.scenario.getId(), getStepUUID(step), stepResult);
        }
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
    public void feature(Feature feature) {
        this.feature = feature;
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
    public void startOfScenarioLifeCycle(Scenario scenario) {
        this.scenario = scenario;
        List<Label> scenarioLables = new ArrayList<>();
        List<Link> scenarioLinks = new ArrayList<>();

        LinkedList<Tag> tags = new LinkedList<>();
        tags.addAll(scenario.getTags());

        scenarioLables.add(getFeatureLabel(feature.getName()));
        scenarioLables.add(getStoryLabel(scenario.getName()));

        while (tags.peek() != null) {
            Tag tag = tags.remove();

            String tagString = tag.getName().toUpperCase();

            if (tagString.contains("=")) {

                String tagKey = tagString.split("=")[0];
                String tagValue = tagString.split("=")[1];

                switch (tagKey) {
                    case SEVERITY:
                        try {
                            scenarioLables.add(getSaverityLabel(tagValue));
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
//                  case LINK:
//                      scenarioLinks.add(ResultsUtils.createLink(null, null, tagValue, null));
//                      break;
                    default:
                        LOG.warn("Composite tag {} is not supported. adding it as RAW", tagKey);
                        scenarioLables.add(new Label().withName("tag").withValue(tag.getName().substring(1)));
                        break;
                }
            } else if (!isResultTag(tag)) {
                scenarioLables.add(new Label().withName("tag")
                        .withValue(tag.getName().substring(1)));
            }
        }

        scenarioLables.add(new Label().withName("host").withValue(getHostName()));
        scenarioLables.add(new Label().withName("package").withValue(feature.getName()));
        scenarioLables.add(new Label().withName("suite").withValue(feature.getName()));
        scenarioLables.add(new Label().withName("testClass").withValue(scenario.getName()));
        scenarioLables.add(new Label().withName("thread").withValue(getThreadName()));

        final TestResult result = new TestResult()
                .withUuid(scenario.getId())
                .withHistoryId(getHistoryId(scenario.getId()))
                .withName(scenario.getName())
                .withLabels(scenarioLables)
                .withLinks(scenarioLinks);

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(scenario.getId());

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
    public void step(Step step) {
        synchronized (gherkinSteps) {
            gherkinSteps.add(step);
        }
    }

    @Override
    public void endOfScenarioLifeCycle(Scenario scenario) {
        lifecycle.stopTestCase(scenario.getId());
        lifecycle.writeTestCase(scenario.getId());
    }

    @Override
    public void done() {
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
            Field step = match.getClass().getDeclaredField("step");
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
        StepResult stepResult = new StepResult();
        stepResult.withName(unimplementedStep.getName())
                .withStart(System.currentTimeMillis())
                .withStop(System.currentTimeMillis())
                .withStatus(Status.SKIPPED)
                .withStatusDetails(new StatusDetails().withMessage("Unimplemented step"));
        lifecycle.startStep(this.scenario.getId(), getStepUUID(unimplementedStep), stepResult);
        lifecycle.stopStep(getStepUUID(unimplementedStep));
    }

    private String getStepUUID(Step step) {
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
        StringWriter stringWriter = new StringWriter();
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

    private Label getSaverityLabel(String severity) {
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
                .filter(tag -> tag.getName().toUpperCase().equals(tagName))
                .findFirst().isPresent();
    }

    private boolean isResultTag(Tag tag) {
        return Arrays.asList(new String[]{FLAKY, KNOWN, MUTED})
                .contains(tag.getName().toUpperCase());
    }
}
