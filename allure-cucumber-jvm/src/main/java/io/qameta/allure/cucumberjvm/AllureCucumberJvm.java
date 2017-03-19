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
import io.qameta.allure.Story;
import io.qameta.allure.model.Label;
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
    private AllureLifecycle lifecycle;
    private Feature feature;
    private Scenario scenario;
    private StepDefinitionMatch match;

    private static final String FAILED = "failed";
    private static final String SKIPPED = "skipped";
    private static final String PASSED = "passed";

    public static final String MD_5 = "md5";

    public AllureCucumberJvm() {
        this.lifecycle = Allure.getLifecycle();
        List<I18n> i18nList = I18n.getAll();

        for (I18n i18n : i18nList) {
            SCENARIO_OUTLINE_KEYWORDS.addAll(i18n.keywords("scenario_outline"));
        }
    }

    @Override
    public void before(Match match, Result result) {
        //Nothing to do with Allure
    }

    @Override
    public void result(Result result) {
        if (match != null) {
            if (FAILED.equals(result.getStatus())) {
                lifecycle.updateStep(stepResult -> stepResult.withStatus(Status.FAILED));
                lifecycle.updateTestCase(scenario.getId(), scenarioResult
                        -> scenarioResult.withStatus(Status.FAILED).
                                withStatusDetails(new StatusDetails()
                                        .withMessage(result.getError().getLocalizedMessage())
                                        .withTrace(getStackTraceAsString(result.getError()))
                                ));
                lifecycle.stopStep();
                lifecycle.stopTestCase(this.scenario.getId());
//                currentStatus = FAILED;
            } else if (SKIPPED.equals(result.getStatus())) {

                lifecycle.updateStep(stepResult -> stepResult.withStatus(Status.SKIPPED));
                lifecycle.stopStep();
                lifecycle.updateTestCase(scenario.getId(), scenarioResult
                        -> scenarioResult.withStatus(Status.SKIPPED)
                                .withStatusDetails(new StatusDetails()
                                        .withMessage("Unimplemented steps were found")));
//                    currentStatus = SKIPPED;
//                }
            } else {
                lifecycle.updateStep(stepResult -> stepResult.withStatus(Status.PASSED));
                lifecycle.stopStep();
                lifecycle.updateTestCase(scenario.getId(), scenarioResult -> scenarioResult.withStatus(Status.PASSED));
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
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void examples(Examples exmpls) {
        //Nothing to do with Allure
    }

    @Override
    public void startOfScenarioLifeCycle(Scenario scenario) {
        this.scenario = scenario;

        List<Label> scenarioLables = new ArrayList<>();
        scenarioLables.add(getFeatureLabel(feature.getName()));
        scenarioLables.add(getStoryLabel(scenario.getName()));
        if (!scenario.getTags().isEmpty()) {
            scenarioLables.add(new Label().withName("tags").withValue(tagsToString(scenario.getTags())));
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
                .withLabels(scenarioLables);

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
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

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
        lifecycle.stopTestCase(scenario.getId());
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

    private String tagsToString(List<Tag> tags) {
        return String.join(", ", tags.stream().map(Tag::getName)
                .collect(Collectors.toList()));
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

    private Label getStoryLabel(String StoryName) {
        return ResultsUtils.createLabel(new Story() {
            @Override
            public String value() {
                return StoryName;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Story.class;
            }
        });

    }
}
