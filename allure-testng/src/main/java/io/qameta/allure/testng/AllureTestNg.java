package io.qameta.allure.testng;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Flaky;
import io.qameta.allure.Muted;
import io.qameta.allure.Owner;
import io.qameta.allure.ResultsUtils;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IAttributes;
import org.testng.IClass;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener2;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.internal.ConstructorOrMethod;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.lang.annotation.Annotation;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.qameta.allure.ResultsUtils.firstNonEmpty;
import static io.qameta.allure.ResultsUtils.getHostName;
import static io.qameta.allure.ResultsUtils.getStatus;
import static io.qameta.allure.ResultsUtils.getStatusDetails;
import static io.qameta.allure.ResultsUtils.getThreadName;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.Entry.comparingByValue;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureTestNg implements ISuiteListener, ITestListener, IInvokedMethodListener2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureTestNg.class);

    private static final String ALLURE_UUID = "ALLURE_UUID";
    private static final String MD_5 = "md5";

    /**
     * Store current test result uuid to attach before/after methods into.
     */
    private final ThreadLocal<Current> currentTestResult
            = InheritableThreadLocal.withInitial(Current::new);

    /**
     * Store current container uuid for fake containers around before/after methods.
     */
    private final ThreadLocal<String> currentTestContainer
            = InheritableThreadLocal.withInitial(() -> UUID.randomUUID().toString());

    /**
     * Store uuid for current executable item to catch steps and attachments.
     */
    private final ThreadLocal<String> currentExecutable
            = InheritableThreadLocal.withInitial(() -> UUID.randomUUID().toString());

    private AllureLifecycle lifecycle;

    public AllureTestNg(AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public AllureTestNg() {
        this.lifecycle = Allure.getLifecycle();
    }

    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }

    @Override
    public void onStart(ISuite suite) {
        LOGGER.info("onStart of " + suite.getName());
        TestResultContainer result = new TestResultContainer()
                .withUuid(getUniqueUuid(suite))
                .withName(suite.getName())
                .withStart(System.currentTimeMillis());
        getLifecycle().startTestContainer(result);
    }

    @Override
    public void onFinish(ISuite suite) {
        LOGGER.info("onFinish of " + suite.getName());
        String uuid = getUniqueUuid(suite);
        getLifecycle().stopTestContainer(uuid);
        getLifecycle().writeTestContainer(uuid);
    }

    @Override
    public void onStart(ITestContext context) {
        LOGGER.info("onStart of " + context.getName());
        String parentUuid = getUniqueUuid(context.getSuite());
        String uuid = getUniqueUuid(context);
        TestResultContainer container = new TestResultContainer()
                .withUuid(uuid)
                .withName(context.getName())
                .withStart(System.currentTimeMillis());
        getLifecycle().startTestContainer(parentUuid, container);
    }

    @Override
    public void onFinish(ITestContext context) {
        LOGGER.info("onFinish of " + context.getName());
        String uuid = getUniqueUuid(context);
        getLifecycle().stopTestContainer(uuid);
        getLifecycle().writeTestContainer(uuid);
    }

    @Override
    public void onTestStart(ITestResult testResult) {
        LOGGER.info("onTestStart of " + testResult.getName());
        Current current = currentTestResult.get();
        if (current.isStarted()) {
            current = refreshContext();
        }
        current.test();
        String parentUuid = getUniqueUuid(testResult.getTestContext());
        ITestNGMethod method = testResult.getMethod();
        final ITestClass testClass = method.getTestClass();
        List<Label> labels = new ArrayList<>();
        labels.addAll(Arrays.asList(
                //Packages grouping
                new Label().withName("package").withValue(testClass.getName()),
                new Label().withName("testClass").withValue(testClass.getName()),
                new Label().withName("testMethod").withValue(method.getMethodName()),

                //xUnit grouping
                new Label().withName("parentSuite").withValue(safeExtractSuiteName(testClass)),
                new Label().withName("suite").withValue(safeExtractTestTag(testClass)),
                new Label().withName("subSuite").withValue(safeExtractTestClassName(testClass)),

                //Timeline grouping
                new Label().withName("host").withValue(getHostName()),
                new Label().withName("thread").withValue(getThreadName())
        ));
        labels.addAll(getLabels(testResult));
        TestResult result = new TestResult()
                .withUuid(current.getUuid())
                .withHistoryId(getHistoryId(method.getQualifiedName(), Collections.emptyMap()))
                .withName(firstNonEmpty(
                        method.getDescription(),
                        method.getMethodName(),
                        method.getQualifiedName()).orElse("Unknown"))
                .withFullName(testResult.getMethod().getQualifiedName())
                .withStatusDetails(new StatusDetails()
                        .withFlaky(isFlaky(testResult))
                        .withMuted(isMuted(testResult)))
                .withParameters(getParameters(testResult))
                .withLinks(getLinks(testResult))
                .withLabels(labels);
        getLifecycle().scheduleTestCase(parentUuid, result);
        getLifecycle().startTestCase(current.getUuid());
    }

    @Override
    public void onTestSuccess(ITestResult testResult) {
        LOGGER.info("onTestSuccess of " + testResult.getName());
        Current current = currentTestResult.get();
        current.after();
        getLifecycle().updateTestCase(current.getUuid(), setStatus(Status.PASSED));
        getLifecycle().stopTestCase(current.getUuid());
        getLifecycle().writeTestCase(current.getUuid());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        LOGGER.info("onTestFailure of " + result.getName());
        Current current = currentTestResult.get();

        if (current.isAfter()) {
            current = refreshContext();
        }

        //if test has failed without any setup
        if (!current.isStarted()) {
            createTestResultForTestWithoutSetup(result);
        }

        current.after();
        Throwable throwable = result.getThrowable();
        Status status = getStatus(throwable).orElse(Status.BROKEN);
        StatusDetails details = getStatusDetails(throwable).orElse(null);
        getLifecycle().updateTestCase(current.getUuid(), setStatus(status, details));
        getLifecycle().stopTestCase(current.getUuid());
        getLifecycle().writeTestCase(current.getUuid());
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        LOGGER.info("onTestSkipped of " + result.getName());
        Current current = currentTestResult.get();

        //test is being skipped as dependent on failed test, closing context for previous test here
        if (current.isAfter()) {
            current = refreshContext();
        }

        //if test was skipped without any setup
        if (!current.isStarted()) {
            createTestResultForTestWithoutSetup(result);
        }
        current.after();
        StatusDetails details = getStatusDetails(result.getThrowable()).orElse(null);
        getLifecycle().updateTestCase(current.getUuid(), setStatus(Status.SKIPPED, details));
        getLifecycle().stopTestCase(current.getUuid());
        getLifecycle().writeTestCase(current.getUuid());
    }

    private void createTestResultForTestWithoutSetup(ITestResult result) {
        onTestStart(result);
        currentTestResult.remove();
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        //do nothing
    }

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult, ITestContext context) {
        ITestNGMethod testMethod = method.getTestMethod();
        LOGGER.info("beforeInvocation2 of {}", testMethod.getMethodName());
        if (isSupportedConfigurationFixture(testMethod)) {
            ifSuiteFixtureStarted(context.getSuite(), testMethod);
            ifTestFixtureStarted(context, testMethod);
            ifMethodFixtureStarted(testMethod);
        }
    }

    private void ifSuiteFixtureStarted(ISuite suite, ITestNGMethod testMethod) {
        if (testMethod.isBeforeSuiteConfiguration()) {
            startBefore(getUniqueUuid(suite), testMethod);
        }
        if (testMethod.isAfterSuiteConfiguration()) {
            startAfter(getUniqueUuid(suite), testMethod);
        }
    }

    private void ifTestFixtureStarted(ITestContext context, ITestNGMethod testMethod) {
        if (testMethod.isBeforeTestConfiguration()) {
            startBefore(getUniqueUuid(context), testMethod);
        }
        if (testMethod.isAfterTestConfiguration()) {
            startAfter(getUniqueUuid(context), testMethod);
        }
    }

    private void startBefore(String parentUuid, ITestNGMethod method) {
        String uuid = currentExecutable.get();
        getLifecycle().startBeforeFixture(parentUuid, uuid, getFixtureResult(method));
    }

    private void startAfter(String parentUuid, ITestNGMethod method) {
        String uuid = currentExecutable.get();
        getLifecycle().startAfterFixture(parentUuid, uuid, getFixtureResult(method));
    }

    private void ifMethodFixtureStarted(ITestNGMethod testMethod) {
        currentTestContainer.remove();
        Current current = currentTestResult.get();
        FixtureResult fixture = getFixtureResult(testMethod);
        String uuid = currentExecutable.get();
        if (testMethod.isBeforeMethodConfiguration()) {
            if (current.isStarted()) {
                currentTestResult.remove();
                current = currentTestResult.get();
            }
            getLifecycle().startBeforeFixture(createFakeContainer(testMethod, current), uuid, fixture);
        }

        if (testMethod.isAfterMethodConfiguration()) {
            getLifecycle().startAfterFixture(createFakeContainer(testMethod, current), uuid, fixture);
        }
    }

    private String createFakeContainer(ITestNGMethod method, Current current) {
        String parentUuid = currentTestContainer.get();
        TestResultContainer container = new TestResultContainer()
                .withUuid(parentUuid)
                .withName(method.getQualifiedName())
                .withStart(System.currentTimeMillis())
                .withDescription(method.getDescription())
                .withChildren(current.getUuid());
        getLifecycle().startTestContainer(container);
        return parentUuid;
    }

    private FixtureResult getFixtureResult(ITestNGMethod method) {
        return new FixtureResult()
                .withName(method.getMethodName())
                .withStart(System.currentTimeMillis())
                .withDescription(method.getDescription())
                .withStage(Stage.RUNNING);
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult, ITestContext context) {
        ITestNGMethod testMethod = method.getTestMethod();
        LOGGER.info("afterInvocation2 of {}", testMethod.getMethodName());
        if (isSupportedConfigurationFixture(testMethod)) {
            String executableUuid = currentExecutable.get();
            currentExecutable.remove();
            getLifecycle().stopFixture(executableUuid);

            if (testMethod.isBeforeMethodConfiguration() || testMethod.isAfterMethodConfiguration()) {
                String containerUuid = currentTestContainer.get();
                validateContainerExists(testMethod.getQualifiedName(), containerUuid);
                currentTestContainer.remove();
                getLifecycle().stopTestContainer(containerUuid);
                getLifecycle().writeTestContainer(containerUuid);
            }
        }
    }

    private boolean isSupportedConfigurationFixture(ITestNGMethod testMethod) {
        return testMethod.isBeforeMethodConfiguration() || testMethod.isAfterMethodConfiguration()
                || testMethod.isBeforeTestConfiguration() || testMethod.isAfterTestConfiguration()
                || testMethod.isBeforeSuiteConfiguration() || testMethod.isAfterSuiteConfiguration();
    }

    private void validateContainerExists(String fixtureName, String containerUuid) {
        if (Objects.isNull(containerUuid)) {
            throw new IllegalStateException(
                    "Could not find container for after method fixture " + fixtureName
            );
        }
    }

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        //do nothing
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        //do nothing
    }

    private List<Label> getLabels(ITestResult result) {
        return Stream.of(
                getAnnotationsOnClass(result, Epic.class).stream().map(ResultsUtils::createLabel),
                getAnnotationsOnMethod(result, Epic.class).stream().map(ResultsUtils::createLabel),
                getAnnotationsOnClass(result, Feature.class).stream().map(ResultsUtils::createLabel),
                getAnnotationsOnMethod(result, Feature.class).stream().map(ResultsUtils::createLabel),
                getAnnotationsOnClass(result, Story.class).stream().map(ResultsUtils::createLabel),
                getAnnotationsOnMethod(result, Story.class).stream().map(ResultsUtils::createLabel),
                getLabels(result, Severity.class, ResultsUtils::createLabel),
                getLabels(result, Owner.class, ResultsUtils::createLabel)
        ).reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toList());
    }

    private List<Link> getLinks(ITestResult result) {
        return Stream.of(
                getAnnotationsOnClass(result, io.qameta.allure.Link.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnMethod(result, io.qameta.allure.Link.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnClass(result, io.qameta.allure.Issue.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnMethod(result, io.qameta.allure.Issue.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnClass(result, io.qameta.allure.TmsLink.class).stream().map(ResultsUtils::createLink),
                getAnnotationsOnMethod(result, io.qameta.allure.TmsLink.class).stream().map(ResultsUtils::createLink)
        ).reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toList());
    }

    private <T extends Annotation> Stream<Label> getLabels(ITestResult result,
                                                           Class<T> clazz, Function<T, Label> extractor) {
        List<Label> onMethod = getAnnotationsOnMethod(result, clazz).stream()
                .map(extractor)
                .collect(Collectors.toList());
        if (!onMethod.isEmpty()) {
            return onMethod.stream();
        }
        return getAnnotationsOnClass(result, clazz).stream()
                .map(extractor);
    }

    private boolean isFlaky(ITestResult result) {
        return hasAnnotation(result, Flaky.class);
    }

    private boolean isMuted(ITestResult result) {
        return hasAnnotation(result, Muted.class);
    }

    private boolean hasAnnotation(ITestResult result, Class<? extends Annotation> clazz) {
        return hasAnnotationOnMethod(result, clazz) || hasAnnotationOnClass(result, clazz);
    }

    private boolean hasAnnotationOnClass(ITestResult result, Class<? extends Annotation> clazz) {
        return !getAnnotationsOnClass(result, clazz).isEmpty();
    }

    private boolean hasAnnotationOnMethod(ITestResult result, Class<? extends Annotation> clazz) {
        return !getAnnotationsOnMethod(result, clazz).isEmpty();
    }

    private <T extends Annotation> List<T> getAnnotationsOnMethod(ITestResult result, Class<T> clazz) {
        return Stream.of(result)
                .map(ITestResult::getMethod)
                .filter(Objects::nonNull)
                .map(ITestNGMethod::getConstructorOrMethod)
                .map(ConstructorOrMethod::getMethod)
                .flatMap(method -> Stream.of(method.getAnnotationsByType(clazz)))
                .collect(Collectors.toList());
    }

    private <T extends Annotation> List<T> getAnnotationsOnClass(ITestResult result, Class<T> clazz) {
        return Stream.of(result)
                .map(ITestResult::getTestClass)
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

    private List<Parameter> getParameters(ITestResult testResult) {
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
