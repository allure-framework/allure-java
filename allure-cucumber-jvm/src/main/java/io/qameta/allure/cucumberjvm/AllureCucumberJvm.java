package io.qameta.allure.cucumberjvm;

import gherkin.formatter.Formatter;
import gherkin.formatter.Reporter;
import gherkin.formatter.model.Background;
import gherkin.formatter.model.Examples;
import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Match;
import gherkin.formatter.model.Result;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.ScenarioOutline;
import gherkin.formatter.model.Step;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Epic;
import io.qameta.allure.Flaky;
import io.qameta.allure.Muted;
import io.qameta.allure.ResultsUtils;
import static io.qameta.allure.ResultsUtils.firstNonEmpty;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import java.lang.annotation.Annotation;
import java.math.BigInteger;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import static java.util.Map.Entry.comparingByValue;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllureCucumberJvm implements Reporter, Formatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureCucumberJvm.class);

    private static final String ALLURE_UUID = "ALLURE_UUID";
    private static final String MD_5 = "md5";

    /**
     * Store current test result uuid to attach before/after methods into.
     */
    private final ThreadLocal<Current> currentTestResult
            = InheritableThreadLocal.withInitial(Current::new);

    /**
     * Store current container uuid for fake containers around before/after
     * methods.
     */
    private final ThreadLocal<String> currentTestContainer
            = InheritableThreadLocal.withInitial(() -> UUID.randomUUID().toString());

    /**
     * Store uuid for current executable item to catch steps and attachments.
     */
    private final ThreadLocal<String> currentExecutable
            = InheritableThreadLocal.withInitial(() -> UUID.randomUUID().toString());

    private AllureLifecycle lifecycle;

    @Override
    public void before(Match match, Result result) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void result(Result result) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void after(Match match, Result result) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void match(Match match) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void embedding(String string, byte[] bytes) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(String string) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void syntaxError(String string, String string1, List<String> list, String string2, Integer intgr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void uri(String string) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void feature(Feature ftr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void scenarioOutline(ScenarioOutline so) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void examples(Examples exmpls) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void startOfScenarioLifeCycle(Scenario scnr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void background(Background b) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void scenario(Scenario scnr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void step(Step step) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void endOfScenarioLifeCycle(Scenario scnr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void done() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void eof() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private List<Label> getLabels(Result result) {
        return Stream.of(
                getAnnotationsOnClass(result, Epic.class).stream().map(ResultsUtils::createLabel),
                getAnnotationsOnMethod(result, Epic.class).stream().map(ResultsUtils::createLabel),
                getAnnotationsOnClass(result, io.qameta.allure.Feature.class).stream().map(ResultsUtils::createLabel),
                getAnnotationsOnMethod(result, io.qameta.allure.Feature.class).stream().map(ResultsUtils::createLabel),
                getAnnotationsOnClass(result, Story.class).stream().map(ResultsUtils::createLabel),
                getAnnotationsOnMethod(result, Story.class).stream().map(ResultsUtils::createLabel),
                getSeverity(result)
        ).reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toList());
    }

    private List<Link> getLinks(Result result) {
        return Stream.of(
                getAnnotationsOnClass(result, io.qameta.allure.Link.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnMethod(result, io.qameta.allure.Link.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnClass(result, io.qameta.allure.Issue.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnMethod(result, io.qameta.allure.Issue.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnClass(result, io.qameta.allure.TmsLink.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnMethod(result, io.qameta.allure.TmsLink.class).stream().map(ResultsUtils::createLink)
        ).reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toList());
    }

    private Stream<Label> getSeverity(Result result) {
        Optional<Label> methodSeverity = getAnnotationsOnMethod(result, Severity.class).stream()
                .map(ResultsUtils::createLabel)
                .findAny();
        if (methodSeverity.isPresent()) {
            return Stream.of(methodSeverity.get());
        }

        Optional<Label> classSeverity = getAnnotationsOnClass(result, Severity.class).stream()
                .map(ResultsUtils::createLabel)
                .findAny();
        if (classSeverity.isPresent()) {
            return Stream.of(classSeverity.get());
        }
        return Stream.empty();
    }

    private boolean isFlaky(Result result) {
        return hasAnnotation(result, Flaky.class);
    }

    private boolean isMuted(Result result) {
        return hasAnnotation(result, Muted.class);
    }

    private boolean hasAnnotation(Result result, Class<? extends Annotation> clazz) {
        return hasAnnotationOnMethod(result, clazz) || hasAnnotationOnClass(result, clazz);
    }

    private boolean hasAnnotationOnClass(Result result, Class<? extends Annotation> clazz) {
        return !getAnnotationsOnClass(result, clazz).isEmpty();
    }

    private boolean hasAnnotationOnMethod(Result result, Class<? extends Annotation> clazz) {
        return !getAnnotationsOnMethod(result, clazz).isEmpty();
    }

    private <T extends Annotation> List<T> getAnnotationsOnMethod(Result result, Class<T> clazz) {
        return Stream.of(result)
                .map(Result::getMethod)
                .filter(Objects::nonNull)
                .map(ITestNGMethod::getConstructorOrMethod)
                .map(ConstructorOrMethod::getMethod)
                .flatMap(method -> Stream.of(method.getAnnotationsByType(clazz)))
                .collect(Collectors.toList());
    }

    private <T extends Annotation> List<T> getAnnotationsOnClass(Result result, Class<T> clazz) {
        return Stream.of(result)
                .map(Result::getTestClass)
                .filter(Objects::nonNull)
                .map(IClass::getRealClass)
                .flatMap(aClass -> Stream.of(aClass.getAnnotationsByType(clazz)))
                .collect(Collectors.toList());
    }

    /**
     * Returns the unique id for given results item.
     */
    private String getUniqueUuid(IAttributes suite) {
        if (Objects.isNull(suite.getAttribute(ALLURE_UUID))) {
            suite.setAttribute(ALLURE_UUID, UUID.randomUUID().toString());
        }
        return Objects.toString(suite.getAttribute(ALLURE_UUID));
    }

    private String getHistoryId(String name, Map<String, String> parameters) {
        MessageDigest digest = getMessageDigest();
        digest.update(name.getBytes(UTF_8));
        parameters.entrySet().stream()
                .sorted(Map.Entry.<String, String>comparingByKey().thenComparing(comparingByValue()))
                .forEachOrdered(entry -> {
                    digest.update(entry.getKey().getBytes(UTF_8));
                    digest.update(entry.getValue().getBytes(UTF_8));
                });
        byte[] bytes = digest.digest();
        return new BigInteger(1, bytes).toString(16);
    }

    private MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance(MD_5);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find md5 hashing algorithm");
        }
    }

    private static String safeExtractSuiteName(ITestClass testClass) {
        Optional<XmlTest> xmlTest = Optional.ofNullable(testClass.getXmlTest());
        return xmlTest.map(XmlTest::getSuite).map(XmlSuite::getName).orElse("Undefined suite");
    }

    private static String safeExtractTestTag(ITestClass testClass) {
        Optional<XmlTest> xmlTest = Optional.ofNullable(testClass.getXmlTest());
        return xmlTest.map(XmlTest::getName).orElse("Undefined test tag");
    }

    private static String safeExtractTestClassName(ITestClass testClass) {
        return firstNonEmpty(testClass.getTestName(), testClass.getName()).orElse("Undefined class name");
    }

    private List<Parameter> getParameters(Result testResult) {
        String[] parameterNames = Stream.of(testResult.getMethod().getConstructorOrMethod().getMethod().getParameters())
                .map(java.lang.reflect.Parameter::getName)
                .toArray(String[]::new);
        String[] parameterValues = Stream.of(testResult.getParameters())
                .map(Objects::toString)
                .toArray(String[]::new);
        return IntStream.range(0, Math.min(parameterNames.length, parameterValues.length))
                .mapToObj(i -> new Parameter().withName(parameterNames[i]).withValue(parameterValues[i]))
                .collect(Collectors.toList());
    }

    private Consumer<TestResult> setStatus(Status status) {
        return result -> result.withStatus(status);
    }

    private Consumer<TestResult> setStatus(Status status, StatusDetails details) {
        return result -> result
                .withStatus(status)
                .withStatusDetails(details);
    }

    private Current refreshContext() {
        currentTestResult.remove();
        return currentTestResult.get();
    }

    private static class Current {

        private final String uuid;
        private CurrentStage currentStage;

        public Current() {
            this.uuid = UUID.randomUUID().toString();
            this.currentStage = CurrentStage.BEFORE;
        }

        public void test() {
            this.currentStage = CurrentStage.TEST;
        }

        public void after() {
            this.currentStage = CurrentStage.AFTER;
        }

        public boolean isStarted() {
            return this.currentStage != CurrentStage.BEFORE;
        }

        public boolean isAfter() {
            return this.currentStage == CurrentStage.AFTER;
        }

        public String getUuid() {
            return uuid;
        }
    }

    private enum CurrentStage {
        BEFORE,
        TEST,
        AFTER
    }

}
