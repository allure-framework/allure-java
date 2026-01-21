/*
 *  Copyright 2016-2024 Qameta Software Inc
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
import io.qameta.allure.testng.config.AllureTestNgConfig;
import io.qameta.allure.util.AnnotationUtils;
import io.qameta.allure.util.ObjectUtils;
import io.qameta.allure.util.ResultsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IAttributes;
import org.testng.IClass;
import org.testng.IConfigurationListener;
import org.testng.IDataProviderListener;
import org.testng.IDataProviderMethod;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.Parameters;
import org.testng.internal.ConstructorOrMethod;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.ALLURE_ID_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.bytesToHex;
import static io.qameta.allure.util.ResultsUtils.createFrameworkLabel;
import static io.qameta.allure.util.ResultsUtils.createHostLabel;
import static io.qameta.allure.util.ResultsUtils.createLanguageLabel;
import static io.qameta.allure.util.ResultsUtils.createPackageLabel;
import static io.qameta.allure.util.ResultsUtils.createParameter;
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
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;

/**
 * Allure TestNG listener.
 */
@SuppressWarnings({
        "ClassDataAbstractionCoupling",
        "ClassFanOutComplexity",
})
public class AllureTestNg implements
        ISuiteListener,
        ITestListener,
        IInvokedMethodListener,
        IConfigurationListener,
        IMethodInterceptor,
        IDataProviderListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureTestNg.class);

    private static final String ALLURE_UUID = "ALLURE_UUID";
    private static final List<Class<?>> INJECTED_TYPES = Arrays.asList(
            ITestContext.class, ITestResult.class, XmlTest.class, Method.class, Object[].class
    );

    private static final boolean HAS_CUCUMBERJVM7_IN_CLASSPATH
            = isClassAvailableOnClasspath("io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm");

    private static final boolean HAS_CUCUMBERJVM6_IN_CLASSPATH
            = isClassAvailableOnClasspath("io.qameta.allure.cucumber6jvm.AllureCucumber6Jvm");

    private static final boolean HAS_CUCUMBERJVM5_IN_CLASSPATH
            = isClassAvailableOnClasspath("io.qameta.allure.cucumber5jvm.AllureCucumber5Jvm");

    private static final boolean HAS_CUCUMBERJVM4_IN_CLASSPATH
            = isClassAvailableOnClasspath("io.qameta.allure.cucumber4jvm.AllureCucumber4Jvm");

    /**
     * Store current testng result uuid to attach before/after methods into.
     */
    private final ThreadLocal<Current> currentTestResult = ThreadLocal
            .withInitial(Current::new);
    /**
     * Store current container uuid for fake containers around before/after methods.
     */
    private final ThreadLocal<String> currentTestContainer = ThreadLocal
            .withInitial(() -> UUID.randomUUID().toString());
    /**
     * Store uuid for current executable item to catch steps and attachments.
     */
    private final ThreadLocal<String> currentExecutable = ThreadLocal
            .withInitial(() -> UUID.randomUUID().toString());
    /**
     * Store uuid for class test containers.
     */
    private final Map<ITestClass, String> classContainerUuidStorage = new ConcurrentHashMap<>();
    /**
     * Store uuid for data provider containers.
     */
    private final Map<ITestNGMethod, String> dataProviderContainerUuidStorage = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final AllureLifecycle lifecycle;
    private final AllureTestNgTestFilter testFilter;

    private final AllureTestNgConfig config;

    /**
     * Package private constructor to allow custom configurations for unit tests.
     */
    AllureTestNg(final AllureLifecycle lifecycle,
                 final AllureTestNgTestFilter testFilter,
                 final AllureTestNgConfig config) {
        this.lifecycle = lifecycle;
        this.testFilter = testFilter;
        this.config = config;
    }

    public AllureTestNg(final AllureLifecycle lifecycle,
                        final AllureTestNgTestFilter testFilter) {
        this.lifecycle = lifecycle;
        this.testFilter = testFilter;
        this.config = AllureTestNgConfig.loadConfigProperties();
    }

    public AllureTestNg(final AllureLifecycle lifecycle) {
        this(lifecycle, new AllureTestNgTestFilter());
    }

    public AllureTestNg() {
        this(Allure.getLifecycle());
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

    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }

    @Override
    public List<IMethodInstance> intercept(final List<IMethodInstance> methods,
                                           final ITestContext context) {
        return testFilter.intercept(methods, context);
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

        if (!config.isHideDisabledTests()) {
            context.getExcludedMethods().stream()
                    .filter(ITestNGMethod::isTest)
                    .filter(method -> !method.getEnabled())
                    .filter(testFilter::isSelected)
                    .forEach(method -> createFakeResult(context, method));
        }
    }

    protected void createFakeResult(final ITestContext context, final ITestNGMethod method) {
        final String uuid = UUID.randomUUID().toString();
        final String parentUuid = UUID.randomUUID().toString();
        startTestCase(context, method, method.getTestClass(), new Object[]{}, parentUuid, uuid);
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
        setClassContainer(testClass, uuid);
    }

    public void onAfterClass(final ITestClass testClass) {
        getClassContainer(testClass).ifPresent(uuid -> {
            getLifecycle().stopTestContainer(uuid);
            getLifecycle().writeTestContainer(uuid);
        });
        dataProviderContainerUuidStorage.entrySet().removeIf(entry -> {
            if (entry.getKey().getTestClass().equals(testClass)) {
                getLifecycle().stopTestContainer(entry.getValue());
                getLifecycle().writeTestContainer(entry.getValue());
                return true;
            }
            return false;
        });
    }

    @Override
    public void onTestStart(final ITestResult testResult) {
        if (shouldSkipReportingFor(testResult)) {
            return;
        }

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
                .ifPresent(clazz -> addClassContainerChild(clazz, uuid));

        Optional.of(testResult)
                .map(ITestResult::getMethod)
                .ifPresent(method -> addDataProviderContainerChild(method, uuid));
    }

    @SuppressWarnings("BooleanExpressionComplexity")
    protected boolean shouldSkipReportingFor(final ITestResult testResult) {
        final String[] groups = Optional.of(testResult)
                .map(ITestResult::getMethod)
                .map(ITestNGMethod::getGroups)
                .orElseGet(() -> new String[]{});

        if (groups.length == 0) {
            return false;
        }

        final Set<String> groupsSet = new HashSet<>(Arrays.asList(groups));
        return (HAS_CUCUMBERJVM7_IN_CLASSPATH
                || HAS_CUCUMBERJVM6_IN_CLASSPATH
                || HAS_CUCUMBERJVM5_IN_CLASSPATH
                || HAS_CUCUMBERJVM4_IN_CLASSPATH
               ) && groupsSet.contains("cucumber");
    }

    protected void startTestCase(final ITestResult testResult,
                                 final String parentUuid,
                                 final String uuid) {
        startTestCase(
                testResult.getTestContext(),
                testResult.getMethod(),
                testResult.getTestClass(),
                testResult.getParameters(),
                parentUuid,
                uuid
        );
    }

    @SuppressWarnings({"Indentation"})
    protected void startTestCase(final ITestContext context,
                                 final ITestNGMethod method,
                                 final IClass iClass,
                                 final Object[] params,
                                 final String parentUuid,
                                 final String uuid) {
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
        labels.addAll(getLabels(method, iClass));
        final List<Parameter> parameters = getParameters(context, method, params);
        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setHistoryId(getHistoryId(method, parameters))
                .setName(getMethodName(method))
                .setFullName(getQualifiedName(method))
                .setStatusDetails(new StatusDetails()
                        .setFlaky(isFlaky(method, iClass))
                        .setMuted(isMuted(method, iClass)))
                .setParameters(parameters)
                .setLinks(getLinks(method, iClass))
                .setLabels(labels);

        processDescription(
                getClass().getClassLoader(),
                method.getConstructorOrMethod().getMethod(),
                result::setDescription,
                result::setDescriptionHtml
        );

        getLifecycle().scheduleTestCase(parentUuid, result);
        getLifecycle().startTestCase(uuid);
    }

    @Override
    public void onTestSuccess(final ITestResult testResult) {
        if (shouldSkipReportingFor(testResult)) {
            return;
        }

        final Current current = currentTestResult.get();
        current.after();
        getLifecycle().updateTestCase(current.getUuid(), setStatus(Status.PASSED));
        getLifecycle().stopTestCase(current.getUuid());
        getLifecycle().writeTestCase(current.getUuid());
    }

    @Override
    public void onTestFailure(final ITestResult result) {
        if (shouldSkipReportingFor(result)) {
            return;
        }

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
        if (shouldSkipReportingFor(result)) {
            return;
        }

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
        final ITestNGMethod testMethod = method.getTestMethod();
        final ITestContext context = testResult.getTestContext();
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
            getClassContainer(testMethod.getTestClass())
                    .ifPresent(parentUuid -> startBefore(parentUuid, testMethod));
        }
        if (testMethod.isAfterClassConfiguration()) {
            getClassContainer(testMethod.getTestClass())
                    .ifPresent(parentUuid -> startAfter(parentUuid, testMethod));
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
                .setChildren(Collections.singletonList(current.getUuid()));
        getLifecycle().startTestContainer(container);
        return parentUuid;
    }

    private String getQualifiedName(final ITestNGMethod method) {
        return method.getRealClass().getName() + "." + method.getMethodName();
    }

    private FixtureResult getFixtureResult(final ITestNGMethod method) {
        final FixtureResult fixtureResult = new FixtureResult()
                .setName(getMethodName(method))
                .setStart(System.currentTimeMillis())
                .setDescription(method.getDescription())
                .setStage(Stage.RUNNING);

        processDescription(
                getClass().getClassLoader(),
                method.getConstructorOrMethod().getMethod(),
                fixtureResult::setDescription,
                fixtureResult::setDescriptionHtml
        );
        return fixtureResult;
    }

    @Override
    public void afterInvocation(final IInvokedMethod method, final ITestResult testResult) {
        final ITestNGMethod testMethod = method.getTestMethod();
        if (isSupportedConfigurationFixture(testMethod)) {
            final String executableUuid = currentExecutable.get();
            currentExecutable.remove();
            if (testResult.isSuccess()) {
                getLifecycle().updateFixture(executableUuid, result -> result.setStatus(Status.PASSED));
            } else {
                getLifecycle().updateFixture(executableUuid, result -> result
                        .setStatus(getStatus(testResult.getThrowable()))
                        .setStatusDetails(getStatusDetails(testResult.getThrowable()).orElse(null)));
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
        if (config.isHideConfigurationFailures()) {
            return; //do nothing
        }

        final String uuid = UUID.randomUUID().toString();
        final String parentUuid = UUID.randomUUID().toString();

        startTestCase(itr, parentUuid, uuid);

        addChildToContainer(getUniqueUuid(itr.getTestContext()), uuid);
        addChildToContainer(getUniqueUuid(itr.getTestContext().getSuite()), uuid);
        addClassContainerChild(itr.getMethod().getTestClass(), uuid);
        // results created for configuration failure should not be considered as test cases.
        getLifecycle().updateTestCase(
                uuid,
                tr -> tr.getLabels().add(
                        new Label().setName(ALLURE_ID_LABEL_NAME).setValue("-1")
                )
        );

        stopTestCase(uuid, itr.getThrowable(), getStatus(itr.getThrowable()));
    }

    @Override
    public void onConfigurationSkip(final ITestResult itr) {
        //do nothing
    }

    @Override
    public void beforeDataProviderExecution(final IDataProviderMethod dataProviderMethod,
                                            final ITestNGMethod method,
                                            final ITestContext iTestContext) {
        final String containerUuid = UUID.randomUUID().toString();
        getLifecycle().startTestContainer(
                new TestResultContainer()
                        .setUuid(containerUuid)
                        .setName(method.getMethodName())
        );
        dataProviderContainerUuidStorage.put(method, containerUuid);

        final String uuid = currentExecutable.get();
        final FixtureResult result = new FixtureResult()
                .setName(dataProviderMethod.getMethod().getName())
                .setStage(Stage.RUNNING);

        processDescription(
                getClass().getClassLoader(),
                dataProviderMethod.getMethod(),
                result::setDescription,
                result::setDescriptionHtml
        );

        getLifecycle().startPrepareFixture(containerUuid, uuid, result);
    }

    @Override
    public void afterDataProviderExecution(final IDataProviderMethod dataProviderMethod,
                                           final ITestNGMethod method,
                                           final ITestContext iTestContext) {
        final String uuid = currentExecutable.get();
        getLifecycle().updateFixture(uuid, result -> {
            if (result.getStatus() == null) {
                result.setStatus(Status.PASSED);
            }
        });
        getLifecycle().stopFixture(uuid);
        currentExecutable.remove();
    }

    @Override
    public void onDataProviderFailure(final ITestNGMethod method,
                                      final ITestContext ctx,
                                      final RuntimeException t) {
        final String uuid = currentExecutable.get();
        getLifecycle().updateFixture(uuid, result -> result
                .setStatus(getStatus(t))
                .setStatusDetails(getStatusDetails(t).orElse(null)));
        getLifecycle().stopFixture(uuid);
        currentExecutable.remove();
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

    private List<Label> getLabels(final ITestNGMethod method, final IClass iClass) {
        final List<Label> labels = new ArrayList<>();
        getMethod(method)
                .map(AnnotationUtils::getLabels)
                .ifPresent(labels::addAll);
        getClass(iClass)
                .map(AnnotationUtils::getLabels)
                .ifPresent(labels::addAll);

        getMethod(method)
                .map(this::getSeverity)
                .filter(Optional::isPresent)
                .orElse(getClass(iClass).flatMap(this::getSeverity))
                .map(ResultsUtils::createSeverityLabel)
                .ifPresent(labels::add);
        return labels;
    }

    private Optional<SeverityLevel> getSeverity(final AnnotatedElement annotatedElement) {
        return Stream.of(annotatedElement.getAnnotationsByType(Severity.class))
                .map(Severity::value)
                .findAny();
    }

    private List<Link> getLinks(final ITestNGMethod method, final IClass iClass) {
        final List<Link> links = new ArrayList<>();
        getMethod(method)
                .map(AnnotationUtils::getLinks)
                .ifPresent(links::addAll);
        getClass(iClass)
                .map(AnnotationUtils::getLinks)
                .ifPresent(links::addAll);
        return links;
    }

    private boolean isFlaky(final ITestNGMethod method, final IClass iClass) {
        final boolean flakyMethod = getMethod(method)
                .map(m -> m.isAnnotationPresent(Flaky.class))
                .orElse(false);
        final boolean flakyClass = getClass(iClass)
                .map(clazz -> clazz.isAnnotationPresent(Flaky.class))
                .orElse(false);
        return flakyMethod || flakyClass;
    }

    private boolean isMuted(final ITestNGMethod method, final IClass iClass) {
        final boolean mutedMethod = getMethod(method)
                .map(m -> m.isAnnotationPresent(Muted.class))
                .orElse(false);
        final boolean mutedClass = getClass(iClass)
                .map(clazz -> clazz.isAnnotationPresent(Muted.class))
                .orElse(false);
        return mutedMethod || mutedClass;
    }

    private Optional<Method> getMethod(final ITestNGMethod method) {
        return Optional.ofNullable(method)
                .map(ITestNGMethod::getConstructorOrMethod)
                .map(ConstructorOrMethod::getMethod);
    }

    private Optional<Class<?>> getClass(final IClass iClass) {
        return Optional.ofNullable(iClass)
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

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private List<Parameter> getParameters(final ITestContext context,
                                          final ITestNGMethod method,
                                          final Object... parameters) {
        final Map<String, String> result = new HashMap<>(
                context.getCurrentXmlTest().getAllParameters()
        );
        final Object instance = method.getInstance();
        if (nonNull(instance)) {
            Stream.of(instance.getClass().getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(TestInstanceParameter.class))
                    .forEach(field -> {
                        final String name = Optional.ofNullable(field.getAnnotation(TestInstanceParameter.class))
                                .map(TestInstanceParameter::value)
                                .filter(s -> !s.isEmpty())
                                .orElseGet(field::getName);
                        try {
                            field.setAccessible(true);
                            final String value = ObjectUtils.toString(field.get(instance));
                            result.put(name, value);
                        } catch (IllegalAccessException e) {
                            LOGGER.debug("Could not access field value");
                        }
                    });
        }

        getMethod(method).ifPresent(m -> {
            final Class<?>[] parameterTypes = m.getParameterTypes();

            if (parameterTypes.length != parameters.length) {
                return;
            }

            final String[] providedNames = Optional.ofNullable(m.getAnnotation(Parameters.class))
                    .map(Parameters::value)
                    .orElse(new String[]{});

            final String[] reflectionNames = Stream.of(m.getParameters())
                    .map(java.lang.reflect.Parameter::getName)
                    .toArray(String[]::new);

            int skippedCount = 0;
            for (int i = 0; i < parameterTypes.length; i++) {
                final Class<?> parameterType = parameterTypes[i];
                if (INJECTED_TYPES.contains(parameterType)) {
                    skippedCount++;
                    continue;
                }

                final int indexFromAnnotation = i - skippedCount;
                if (indexFromAnnotation < providedNames.length) {
                    result.put(providedNames[indexFromAnnotation], ObjectUtils.toString(parameters[i]));
                    continue;
                }

                if (i < reflectionNames.length) {
                    result.put(reflectionNames[i], ObjectUtils.toString(parameters[i]));
                }
            }

        });

        return result.entrySet().stream()
                .map(entry -> createParameter(entry.getKey(), entry.getValue()))
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
            if (nonNull(details)) {
                result.getStatusDetails().setTrace(details.getTrace());
                result.getStatusDetails().setMessage(details.getMessage());
            }
        };
    }

    private void addDataProviderContainerChild(final ITestNGMethod method, final String childUuid) {
        this.addChildToContainer(dataProviderContainerUuidStorage.get(method), childUuid);
    }

    private void addClassContainerChild(final ITestClass clazz, final String childUuid) {
        this.addChildToContainer(classContainerUuidStorage.get(clazz), childUuid);
    }

    private void addChildToContainer(final String containerUuid, final String childUuid) {
        lock.writeLock().lock();
        try {
            if (nonNull(containerUuid)) {
                getLifecycle().updateTestContainer(
                        containerUuid,
                        container -> container.getChildren().add(childUuid)
                );
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Optional<String> getClassContainer(final ITestClass clazz) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(classContainerUuidStorage.get(clazz));
        } finally {
            lock.readLock().unlock();
        }
    }

    private void setClassContainer(final ITestClass clazz, final String uuid) {
        lock.writeLock().lock();
        try {
            classContainerUuidStorage.put(clazz, uuid);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Current refreshContext() {
        currentTestResult.remove();
        return currentTestResult.get();
    }

    private static boolean isClassAvailableOnClasspath(final String clazz) {
        try {
            AllureTestNg.class.getClassLoader().loadClass(clazz);
            return true;
        } catch (Exception ignored) {
            return false;
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

}
