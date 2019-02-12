/*
 *  Copyright 2019 Qameta Software OÜ
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.testng;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Flaky;
import io.qameta.allure.Muted;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.util.AnnotationUtils;
import io.qameta.allure.util.ObjectUtils;
import io.qameta.allure.util.ResultsUtils;
import org.testng.IAttributes;
import org.testng.IClass;
import org.testng.IConfigurationListener;
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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.bytesToHex;
import static io.qameta.allure.util.ResultsUtils.createFrameworkLabel;
import static io.qameta.allure.util.ResultsUtils.createHostLabel;
import static io.qameta.allure.util.ResultsUtils.createLanguageLabel;
import static io.qameta.allure.util.ResultsUtils.createPackageLabel;
import static io.qameta.allure.util.ResultsUtils.createParentSuiteLabel;
import static io.qameta.allure.util.ResultsUtils.createSubSuiteLabel;
import static io.qameta.allure.util.ResultsUtils.createSuiteLabel;
import static io.qameta.allure.util.ResultsUtils.createTestClassLabel;
import static io.qameta.allure.util.ResultsUtils.createTestMethodLabel;
import static io.qameta.allure.util.ResultsUtils.createThreadLabel;
import static io.qameta.allure.util.ResultsUtils.firstNonEmpty;
import static io.qameta.allure.util.ResultsUtils.getMd5Digest;
import static io.qameta.allure.util.ResultsUtils.getProvidedLabels;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static io.qameta.allure.util.ResultsUtils.processDescription;
import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparing;
import static java.util.stream.IntStream.range;

/**
 * Allure TestNG listener.
 */
@SuppressWarnings({
        "PMD.ExcessiveImports", "PMD.TooManyMethods", "PMD.GodClass",
        "ClassFanOutComplexity", "ClassDataAbstractionCoupling", "PMD.ExcessiveClassLength"
})
public class AllureTestNg implements
        ISuiteListener,
        ITestListener,
        IInvokedMethodListener2,
        IConfigurationListener {

    private static final String ALLURE_UUID = "ALLURE_UUID";

    /**
     * Store current testng result uuid to attach before/after methods into.
     */
    private final ThreadLocal<Current> currentTestResult = new InheritableThreadLocal<Current>() {
        @Override
        protected Current initialValue() {
            return new Current();
        }
    };

    /**
     * Store current container uuid for fake containers around before/after methods.
     */
    private final ThreadLocal<String> currentTestContainer = new InheritableThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return UUID.randomUUID().toString();
        }
    };

    /**
     * Store uuid for current executable item to catch steps and attachments.
     */
    private final ThreadLocal<String> currentExecutable = new InheritableThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return UUID.randomUUID().toString();
        }
    };

    /**
     * Store uuid for class test containers.
     */
    private final Map<ITestClass, String> classContainerUuidStorage = new ConcurrentHashMap<>();

    private final AllureLifecycle lifecycle;

    public AllureTestNg(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public AllureTestNg() {
        this.lifecycle = Allure.getLifecycle();
    }

    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }

    @Override
    public void onStart(final ISuite suite) {
        final TestResultContainer result = new TestResultContainer()
                .setUuid(getUniqueUuid(suite))
                .setName(suite.getName())
                .setStart(System.currentTimeMillis());
        getLifecycle().startTestContainer(result);
    }

    @Override
    public void onStart(final ITestContext context) {
        final String parentUuid = getUniqueUuid(context.getSuite());
        final String uuid = getUniqueUuid(context);
        final TestResultContainer container = new TestResultContainer()
                .setUuid(uuid)
                .setName(context.getName())
                .setStart(System.currentTimeMillis());
        getLifecycle().startTestContainer(parentUuid, container);

        Stream.of(context.getAllTestMethods())
                .map(ITestNGMethod::getTestClass)
                .distinct()
                .forEach(this::onBeforeClass);

        context.getExcludedMethods().stream()
                .filter(ITestNGMethod::isTest)
                .filter(method -> !method.getEnabled())
                .forEach(method -> createFakeResult(context, method));
    }

    protected void createFakeResult(final ITestContext context, final ITestNGMethod method) {
        final org.testng.internal.TestResult result = new org.testng.internal.TestResult(
                new Object(), method, null, context
        );
        final String uuid = UUID.randomUUID().toString();
        final String parentUuid = UUID.randomUUID().toString();
        startTestCase(result, parentUuid, uuid);
        stopTestCase(uuid, null, null);
    }

    @Override
    public void onFinish(final ISuite suite) {
        final String uuid = getUniqueUuid(suite);
        getLifecycle().stopTestContainer(uuid);
        getLifecycle().writeTestContainer(uuid);

    }

    @Override
    public void onFinish(final ITestContext context) {
        final String uuid = getUniqueUuid(context);
        getLifecycle().stopTestContainer(uuid);
        getLifecycle().writeTestContainer(uuid);

        Stream.of(context.getAllTestMethods())
                .map(ITestNGMethod::getTestClass)
                .distinct()
                .forEach(this::onAfterClass);
    }

    public void onBeforeClass(final ITestClass testClass) {
        final String uuid = UUID.randomUUID().toString();
        final TestResultContainer container = new TestResultContainer()
                .setUuid(uuid)
                .setName(testClass.getName());
        getLifecycle().startTestContainer(container);
        classContainerUuidStorage.put(testClass, uuid);
    }

    public void onAfterClass(final ITestClass testClass) {
        if (!classContainerUuidStorage.containsKey(testClass)) {
            return;
        }
        final String uuid = classContainerUuidStorage.get(testClass);
        getLifecycle().stopTestContainer(uuid);
        getLifecycle().writeTestContainer(uuid);
    }

    @Override
    public void onTestStart(final ITestResult testResult) {
        Current current = currentTestResult.get();
        if (current.isStarted()) {
            current = refreshContext();
        }
        current.test();
        final String uuid = current.getUuid();
        final String parentUuid = getUniqueUuid(testResult.getTestContext());

        startTestCase(testResult, parentUuid, uuid);

        Optional.of(testResult)
                .map(ITestResult::getMethod)
                .map(ITestNGMethod::getTestClass)
                .map(classContainerUuidStorage::get)
                .ifPresent(testClassContainerUuid -> getLifecycle().updateTestContainer(
                        testClassContainerUuid,
                        container -> container.getChildren().add(uuid)
                ));
    }

    @SuppressWarnings({"Indentation", "PMD.ExcessiveMethodLength", "deprecation"})
    protected void startTestCase(final ITestResult testResult,
                                 final String parentUuid,
                                 final String uuid) {
        final ITestNGMethod method = testResult.getMethod();
        final ITestClass testClass = method.getTestClass();
        final List<Label> labels = new ArrayList<>();
        labels.addAll(getProvidedLabels());
        labels.addAll(Arrays.asList(
                //Packages grouping
                createPackageLabel(testClass.getName()),
                createTestClassLabel(testClass.getName()),
                createTestMethodLabel(method.getMethodName()),

                //xUnit grouping
                createParentSuiteLabel(safeExtractSuiteName(testClass)),
                createSuiteLabel(safeExtractTestTag(testClass)),
                createSubSuiteLabel(safeExtractTestClassName(testClass)),

                //Timeline grouping
                createHostLabel(),
                createThreadLabel(),

                createFrameworkLabel("testng"),
                createLanguageLabel("java")
        ));
        labels.addAll(getLabels(testResult));
        final List<Parameter> parameters = getParameters(testResult);
        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setHistoryId(getHistoryId(method, parameters))
                .setName(getMethodName(method))
                .setFullName(getQualifiedName(method))
                .setStatusDetails(new StatusDetails()
                        .setFlaky(isFlaky(testResult))
                        .setMuted(isMuted(testResult)))
                .setParameters(parameters)
                .setLinks(getLinks(testResult))
                .setLabels(labels);
        processDescription(getClass().getClassLoader(), method.getConstructorOrMethod().getMethod(), result);
        getLifecycle().scheduleTestCase(parentUuid, result);
        getLifecycle().startTestCase(uuid);
    }

    @Override
    public void onTestSuccess(final ITestResult testResult) {
        final Current current = currentTestResult.get();
        current.after();
        getLifecycle().updateTestCase(current.getUuid(), setStatus(Status.PASSED));
        getLifecycle().stopTestCase(current.getUuid());
        getLifecycle().writeTestCase(current.getUuid());
    }

    @Override
    public void onTestFailure(final ITestResult result) {
        Current current = currentTestResult.get();

        if (current.isAfter()) {
            current = refreshContext();
        }

        //if testng has failed without any setup
        if (!current.isStarted()) {
            createTestResultForTestWithoutSetup(result);
        }

        current.after();
        final String uuid = current.getUuid();

        final Throwable throwable = result.getThrowable();
        final Status status = getStatus(throwable);
        stopTestCase(uuid, throwable, status);
    }

    protected void stopTestCase(final String uuid, final Throwable throwable, final Status status) {
        final StatusDetails details = getStatusDetails(throwable).orElse(null);
        getLifecycle().updateTestCase(uuid, setStatus(status, details));
        getLifecycle().stopTestCase(uuid);
        getLifecycle().writeTestCase(uuid);
    }

    @Override
    public void onTestSkipped(final ITestResult result) {
        Current current = currentTestResult.get();

        //testng is being skipped as dependent on failed testng, closing context for previous testng here
        if (current.isAfter()) {
            current = refreshContext();
        }

        //if testng was skipped without any setup
        if (!current.isStarted()) {
            createTestResultForTestWithoutSetup(result);
        }
        current.after();
        stopTestCase(current.getUuid(), result.getThrowable(), Status.SKIPPED);
    }

    private void createTestResultForTestWithoutSetup(final ITestResult result) {
        onTestStart(result);
        currentTestResult.remove();
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(final ITestResult result) {
        //do nothing
    }

    @Override
    public void beforeInvocation(final IInvokedMethod method, final ITestResult testResult) {
        //do nothing
    }

    @Override
    public void beforeInvocation(final IInvokedMethod method, final ITestResult testResult,
                                 final ITestContext context) {
        final ITestNGMethod testMethod = method.getTestMethod();
        if (isSupportedConfigurationFixture(testMethod)) {
            ifSuiteFixtureStarted(context.getSuite(), testMethod);
            ifTestFixtureStarted(context, testMethod);
            ifClassFixtureStarted(testMethod);
            ifMethodFixtureStarted(testMethod);
        }
    }

    private void ifSuiteFixtureStarted(final ISuite suite, final ITestNGMethod testMethod) {
        if (testMethod.isBeforeSuiteConfiguration()) {
            startBefore(getUniqueUuid(suite), testMethod);
        }
        if (testMethod.isAfterSuiteConfiguration()) {
            startAfter(getUniqueUuid(suite), testMethod);
        }
    }

    private void ifClassFixtureStarted(final ITestNGMethod testMethod) {
        if (testMethod.isBeforeClassConfiguration()) {
            final String parentUuid = classContainerUuidStorage.get(testMethod.getTestClass());
            startBefore(parentUuid, testMethod);
        }
        if (testMethod.isAfterClassConfiguration()) {
            final String parentUuid = classContainerUuidStorage.get(testMethod.getTestClass());
            startAfter(parentUuid, testMethod);
        }
    }

    private void ifTestFixtureStarted(final ITestContext context, final ITestNGMethod testMethod) {
        if (testMethod.isBeforeTestConfiguration()) {
            startBefore(getUniqueUuid(context), testMethod);
        }
        if (testMethod.isAfterTestConfiguration()) {
            startAfter(getUniqueUuid(context), testMethod);
        }
    }

    private void startBefore(final String parentUuid, final ITestNGMethod method) {
        final String uuid = currentExecutable.get();
        getLifecycle().startPrepareFixture(parentUuid, uuid, getFixtureResult(method));
    }

    private void startAfter(final String parentUuid, final ITestNGMethod method) {
        final String uuid = currentExecutable.get();
        getLifecycle().startTearDownFixture(parentUuid, uuid, getFixtureResult(method));
    }

    private void ifMethodFixtureStarted(final ITestNGMethod testMethod) {
        currentTestContainer.remove();
        Current current = currentTestResult.get();
        final FixtureResult fixture = getFixtureResult(testMethod);
        final String uuid = currentExecutable.get();
        if (testMethod.isBeforeMethodConfiguration()) {
            if (current.isStarted()) {
                currentTestResult.remove();
                current = currentTestResult.get();
            }
            getLifecycle().startPrepareFixture(createFakeContainer(testMethod, current), uuid, fixture);
        }

        if (testMethod.isAfterMethodConfiguration()) {
            getLifecycle().startTearDownFixture(createFakeContainer(testMethod, current), uuid, fixture);
        }
    }

    private String createFakeContainer(final ITestNGMethod method, final Current current) {
        final String parentUuid = currentTestContainer.get();
        final TestResultContainer container = new TestResultContainer()
                .setUuid(parentUuid)
                .setName(getQualifiedName(method))
                .setStart(System.currentTimeMillis())
                .setDescription(method.getDescription())
                .setChildren(current.getUuid());
        getLifecycle().startTestContainer(container);
        return parentUuid;
    }

    private String getQualifiedName(final ITestNGMethod method) {
        return method.getRealClass().getName() + "." + method.getMethodName();
    }

    @SuppressWarnings("deprecation")
    private FixtureResult getFixtureResult(final ITestNGMethod method) {
        final FixtureResult fixtureResult = new FixtureResult()
                .withName(getMethodName(method))
                .withStart(System.currentTimeMillis())
                .withDescription(method.getDescription())
                .withStage(Stage.RUNNING);
        processDescription(getClass().getClassLoader(), method.getConstructorOrMethod().getMethod(), fixtureResult);
        return fixtureResult;
    }

    @Override
    public void afterInvocation(final IInvokedMethod method, final ITestResult testResult) {
        //do nothing
    }

    @Override
    public void afterInvocation(final IInvokedMethod method, final ITestResult testResult,
                                final ITestContext context) {
        final ITestNGMethod testMethod = method.getTestMethod();
        if (isSupportedConfigurationFixture(testMethod)) {
            final String executableUuid = currentExecutable.get();
            currentExecutable.remove();
            if (testResult.isSuccess()) {
                getLifecycle().updateFixture(executableUuid, result -> result.withStatus(Status.PASSED));
            } else {
                getLifecycle().updateFixture(executableUuid, result -> result
                        .withStatus(getStatus(testResult.getThrowable()))
                        .withStatusDetails(getStatusDetails(testResult.getThrowable()).orElse(null)));
            }
            getLifecycle().stopFixture(executableUuid);

            if (testMethod.isBeforeMethodConfiguration() || testMethod.isAfterMethodConfiguration()) {
                final String containerUuid = currentTestContainer.get();
                validateContainerExists(getQualifiedName(testMethod), containerUuid);
                currentTestContainer.remove();
                getLifecycle().stopTestContainer(containerUuid);
                getLifecycle().writeTestContainer(containerUuid);
            }
        }
    }

    @Override
    public void onConfigurationSuccess(final ITestResult itr) {
        //do nothing
    }

    @Override
    public void onConfigurationFailure(final ITestResult itr) {
        final String uuid = UUID.randomUUID().toString();
        final String parentUuid = UUID.randomUUID().toString();

        startTestCase(itr, parentUuid, uuid);
        stopTestCase(uuid, itr.getThrowable(), getStatus(itr.getThrowable()));
        //do nothing
    }

    @Override
    public void onConfigurationSkip(final ITestResult itr) {
        //do nothing
    }

    protected String getHistoryId(final ITestNGMethod method, final List<Parameter> parameters) {
        final MessageDigest digest = getMd5Digest();
        final String testClassName = method.getTestClass().getName();
        final String methodName = method.getMethodName();
        digest.update(testClassName.getBytes(UTF_8));
        digest.update(methodName.getBytes(UTF_8));
        parameters.stream()
                .sorted(comparing(Parameter::getName).thenComparing(Parameter::getValue))
                .forEachOrdered(parameter -> {
                    digest.update(parameter.getName().getBytes(UTF_8));
                    digest.update(parameter.getValue().getBytes(UTF_8));
                });
        final byte[] bytes = digest.digest();
        return bytesToHex(bytes);
    }

    protected Status getStatus(final Throwable throwable) {
        return ResultsUtils.getStatus(throwable).orElse(Status.BROKEN);
    }

    @SuppressWarnings("BooleanExpressionComplexity")
    private boolean isSupportedConfigurationFixture(final ITestNGMethod testMethod) {
        return testMethod.isBeforeMethodConfiguration() || testMethod.isAfterMethodConfiguration()
                || testMethod.isBeforeTestConfiguration() || testMethod.isAfterTestConfiguration()
                || testMethod.isBeforeClassConfiguration() || testMethod.isAfterClassConfiguration()
                || testMethod.isBeforeSuiteConfiguration() || testMethod.isAfterSuiteConfiguration();
    }

    private void validateContainerExists(final String fixtureName, final String containerUuid) {
        if (Objects.isNull(containerUuid)) {
            throw new IllegalStateException(
                    "Could not find container for after method fixture " + fixtureName
            );
        }
    }

    private List<Label> getLabels(final ITestResult result) {
        final List<Label> labels = new ArrayList<>();
        getMethod(result)
                .map(AnnotationUtils::getLabels)
                .ifPresent(labels::addAll);
        getClass(result)
                .map(AnnotationUtils::getLabels)
                .ifPresent(labels::addAll);

        getMethod(result)
                .map(this::getSeverity)
                .filter(Optional::isPresent)
                .orElse(getClass(result).flatMap(this::getSeverity))
                .map(ResultsUtils::createSeverityLabel)
                .ifPresent(labels::add);
        return labels;
    }

    private Optional<SeverityLevel> getSeverity(final AnnotatedElement annotatedElement) {
        return Stream.of(annotatedElement.getAnnotationsByType(Severity.class))
                .map(Severity::value)
                .findAny();
    }

    private List<Link> getLinks(final ITestResult result) {
        final List<Link> links = new ArrayList<>();
        getMethod(result)
                .map(AnnotationUtils::getLinks)
                .ifPresent(links::addAll);
        getClass(result)
                .map(AnnotationUtils::getLinks)
                .ifPresent(links::addAll);
        return links;
    }

    private boolean isFlaky(final ITestResult result) {
        final boolean flakyMethod = getMethod(result)
                .map(method -> method.isAnnotationPresent(Flaky.class))
                .orElse(false);
        final boolean flakyClass = getClass(result)
                .map(clazz -> clazz.isAnnotationPresent(Flaky.class))
                .orElse(false);
        return flakyMethod || flakyClass;
    }

    private boolean isMuted(final ITestResult result) {
        final boolean mutedMethod = getMethod(result)
                .map(method -> method.isAnnotationPresent(Muted.class))
                .orElse(false);
        final boolean mutedClass = getClass(result)
                .map(clazz -> clazz.isAnnotationPresent(Muted.class))
                .orElse(false);
        return mutedMethod || mutedClass;
    }

    private Optional<Method> getMethod(final ITestResult result) {
        return Optional.of(result)
                .map(ITestResult::getMethod)
                .map(ITestNGMethod::getConstructorOrMethod)
                .map(ConstructorOrMethod::getMethod);
    }

    private Optional<Class<?>> getClass(final ITestResult result) {
        return Optional.of(result)
                .map(ITestResult::getTestClass)
                .map(IClass::getRealClass);
    }

    /**
     * Returns the unique id for given results item.
     */
    private String getUniqueUuid(final IAttributes suite) {
        if (Objects.isNull(suite.getAttribute(ALLURE_UUID))) {
            suite.setAttribute(ALLURE_UUID, UUID.randomUUID().toString());
        }
        return Objects.toString(suite.getAttribute(ALLURE_UUID));
    }

    private static String safeExtractSuiteName(final ITestClass testClass) {
        final Optional<XmlTest> xmlTest = Optional.ofNullable(testClass.getXmlTest());
        return xmlTest.map(XmlTest::getSuite).map(XmlSuite::getName).orElse("Undefined suite");
    }

    private static String safeExtractTestTag(final ITestClass testClass) {
        final Optional<XmlTest> xmlTest = Optional.ofNullable(testClass.getXmlTest());
        return xmlTest.map(XmlTest::getName).orElse("Undefined testng tag");
    }

    private static String safeExtractTestClassName(final ITestClass testClass) {
        return firstNonEmpty(testClass.getTestName(), testClass.getName()).orElse("Undefined class name");
    }

    private List<Parameter> getParameters(final ITestResult testResult) {
        final Stream<Parameter> tagsParameters = testResult.getTestContext()
                .getCurrentXmlTest().getAllParameters().entrySet()
                .stream()
                .map(entry -> new Parameter().setName(entry.getKey()).setValue(entry.getValue()));
        final String[] parameterNames = Optional.of(testResult)
                .map(ITestResult::getMethod)
                .map(ITestNGMethod::getConstructorOrMethod)
                .map(ConstructorOrMethod::getMethod)
                .map(Executable::getParameters)
                .map(Stream::of)
                .orElseGet(Stream::empty)
                .map(java.lang.reflect.Parameter::getName)
                .toArray(String[]::new);
        final String[] parameterValues = Stream.of(testResult.getParameters())
                .map(ObjectUtils::toString)
                .toArray(String[]::new);
        final Stream<Parameter> methodParameters = range(0, min(parameterNames.length, parameterValues.length))
                .mapToObj(i -> new Parameter().setName(parameterNames[i]).setValue(parameterValues[i]));
        return Stream.concat(tagsParameters, methodParameters)
                .collect(Collectors.toList());
    }

    private String getMethodName(final ITestNGMethod method) {
        return firstNonEmpty(
                method.getDescription(),
                method.getMethodName(),
                getQualifiedName(method)).orElse("Unknown");
    }

    @SuppressWarnings("SameParameterValue")
    private Consumer<TestResult> setStatus(final Status status) {
        return result -> result.setStatus(status);
    }

    private Consumer<TestResult> setStatus(final Status status, final StatusDetails details) {
        return result -> {
            result.setStatus(status);
            if (Objects.nonNull(details)) {
                result.getStatusDetails().setTrace(details.getTrace());
                result.getStatusDetails().setMessage(details.getMessage());
            }
        };
    }

    private Current refreshContext() {
        currentTestResult.remove();
        return currentTestResult.get();
    }

    /**
     * Describes current test result.
     */
    private static class Current {
        private final String uuid;
        private CurrentStage currentStage;

        Current() {
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

    /**
     * The stage of current result context.
     */
    private enum CurrentStage {
        BEFORE,
        TEST,
        AFTER
    }
}
