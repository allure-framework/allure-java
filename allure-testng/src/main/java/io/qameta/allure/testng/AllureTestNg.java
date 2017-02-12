package io.qameta.allure.testng;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestAfterResult;
import io.qameta.allure.model.TestBeforeResult;
import io.qameta.allure.model.TestGroupResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IAttributes;
import org.testng.IClassListener;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener2;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;

import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.qameta.allure.ResultsUtils.getStatus;
import static io.qameta.allure.ResultsUtils.getStatusDetails;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureTestNg implements ISuiteListener, ITestListener, IClassListener, IInvokedMethodListener2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureTestNg.class);

    private static final String ALLURE_UUID = "ALLURE_UUID";
    private static final StatusDetails WITHOUT_REASON = new StatusDetails().withMessage("Without a reason");
    private static final String MD_5 = "md5";

    private enum TestState {PREPARING, STARTED}

    private final ThreadLocal<String> threadLocalUuid
            = InheritableThreadLocal.withInitial(() -> UUID.randomUUID().toString());

    private final ThreadLocal<String> currentTestCaseUuid
            = InheritableThreadLocal.withInitial(() -> UUID.randomUUID().toString());

    private final ConcurrentHashMap<String, TestState> testCasesStorage = new ConcurrentHashMap<>();

    //    SUITE
    @Override
    public void onStart(ISuite suite) {
        LOGGER.info("onStart of " + suite.getName());
        TestGroupResult result = new TestGroupResult()
                .withId(getId(suite))
                .withName(suite.getName());
        Allure.LIFECYCLE.startTestGroup(getUuid(suite), result);
    }

    @Override
    public void onFinish(ISuite suite) {
        LOGGER.info("onFinish of " + suite.getName());
        Allure.LIFECYCLE.stopTestGroup(getUuid(suite));
    }

    //    TEST
    @Override
    public void onStart(ITestContext context) {
        LOGGER.info("onStart of " + context.getName());
        TestGroupResult result = new TestGroupResult()
                .withId(getId(context))
                .withParentIds(getId(context.getSuite()))
                .withName(context.getName());
        Allure.LIFECYCLE.startTestGroup(getUuid(context), result);
    }

    @Override
    public void onFinish(ITestContext context) {
        LOGGER.info("onFinish of " + context.getName());
        saveAllTestResults(context.getPassedTests().getAllResults());
        saveAllTestResults(context.getSkippedTests().getAllResults());
        saveAllTestResults(context.getFailedTests().getAllResults());
        saveAllTestResults(context.getFailedButWithinSuccessPercentageTests().getAllResults());
        Allure.LIFECYCLE.stopTestGroup(getUuid(context));
    }

    //    CLASS
    @Override
    public void onBeforeClass(ITestClass testClass) {
        LOGGER.info("onBeforeClass of " + testClass.getName());
    }

    @Override
    public void onAfterClass(ITestClass testClass) {
        LOGGER.info("onAfterClass of " + testClass.getName());
    }

    //    METHOD
    @Override
    public void onTestStart(ITestResult testResult) {
        LOGGER.info("onTestStart of " + testResult.getName());
        String uuid = currentTestCaseUuid.get();
        testCasesStorage.put(uuid, TestState.STARTED);
        testResult.setAttribute(ALLURE_UUID, uuid);
        List<Parameter> parameters = getParameters(testResult);
        updateTestCase(testResult);
        Allure.LIFECYCLE.updateTestCase(uuid, t -> t.withParameters(parameters));
        Allure.LIFECYCLE.startTestCase(uuid);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        LOGGER.info("onTestSuccess of " + result.getName());
        String uuid = getUuid(result);
        Allure.LIFECYCLE.updateTestCase(uuid, t -> t
                .withStatus(Status.PASSED)
        );
        Allure.LIFECYCLE.stopTestCase(uuid);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        LOGGER.info("onTestFailure of " + result.getName());
        String uuid = currentTestCaseUuid.get();
        Throwable throwable = result.getThrowable();
        Allure.LIFECYCLE.updateTestCase(uuid, t -> t
                .withStatus(getStatus(throwable).orElse(Status.BROKEN))
                .withStatusDetails(getStatusDetails(throwable).orElse(WITHOUT_REASON))
        );
        Allure.LIFECYCLE.stopTestCase(uuid);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        LOGGER.info("onTestSkipped of " + result.getName());
        String uuid = getUuid(result);
        //in case test was skipped without any setup
        testCasesStorage.putIfAbsent(uuid, TestState.STARTED);
        updateTestCase(result);
        StatusDetails details = getStatusDetails(result.getThrowable()).orElse(WITHOUT_REASON);
        Allure.LIFECYCLE.cancelTestCase(uuid, details);
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        LOGGER.info("onTestFailedButWithinSuccessPercentage of " + result.getName());
        String uuid = currentTestCaseUuid.get();
        StatusDetails details = new StatusDetails().withMessage("Skipped without a reason");
        Allure.LIFECYCLE.updateTestCase(uuid, t -> t.withStatus(Status.CANCELED).withStatusDetails(details));
        Allure.LIFECYCLE.stopTestCase(uuid);
    }

    //    CONFIGURATION METHODS
    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult, ITestContext context) {
        ITestNGMethod testMethod = method.getTestMethod();
        LOGGER.info("beforeInvocation2 of {}", testMethod.getMethodName());
        if (method.isConfigurationMethod()) {
            if (testMethod.isBeforeSuiteConfiguration()) {
                startBefore(getUuid(context.getSuite()), testMethod);
            }
            if (testMethod.isAfterSuiteConfiguration()) {
                startAfter(getUuid(context.getSuite()), testMethod);
            }
            if (testMethod.isBeforeTestConfiguration()) {
                startBefore(getUuid(context), testMethod);
            }
            if (testMethod.isAfterTestConfiguration()) {
                startAfter(getUuid(context), testMethod);
            }
            if (testMethod.isBeforeMethodConfiguration()) {
                String uuid = currentTestCaseUuid.get();
                if (testCasesStorage.containsKey(uuid) && testCasesStorage.get(uuid) == TestState.STARTED) {
                    LOGGER.info("Ending test case " + uuid);
                    Allure.LIFECYCLE.closeTestCase(uuid);
                    testCasesStorage.remove(uuid);
                    currentTestCaseUuid.remove();
                }
                startBefore(currentTestCaseUuid.get(), testMethod);
                testCasesStorage.put(currentTestCaseUuid.get(), TestState.PREPARING);
            }
            if (testMethod.isAfterMethodConfiguration()) {
                startAfter(currentTestCaseUuid.get(), testMethod);
            }
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult, ITestContext context) {
        ITestNGMethod testMethod = method.getTestMethod();
        LOGGER.info("afterInvocation2 of {}", testMethod.getMethodName());
        if (method.isConfigurationMethod()) {
            if (testMethod.isBeforeSuiteConfiguration() || testMethod.isBeforeTestConfiguration()
                    || testMethod.isBeforeMethodConfiguration()) {
                String uuid = threadLocalUuid.get();
                threadLocalUuid.remove();
                Allure.LIFECYCLE.stopTestBefore(uuid);
            }
            if (testMethod.isAfterSuiteConfiguration() || testMethod.isAfterTestConfiguration()) {
                String uuid = threadLocalUuid.get();
                threadLocalUuid.remove();
                Allure.LIFECYCLE.stopTestAfter(uuid);
            }
            if (testMethod.isAfterMethodConfiguration()) {
                String uuid = threadLocalUuid.get();
                threadLocalUuid.remove();
                Allure.LIFECYCLE.stopTestAfter(uuid);
            }
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

    private void saveAllTestResults(Set<ITestResult> results) {
        for (ITestResult result : results) {
            String uuid = getUuid(result);
            if (testCasesStorage.containsKey(uuid) && testCasesStorage.get(uuid) == TestState.STARTED) {
                Allure.LIFECYCLE.closeTestCase(uuid);
                testCasesStorage.remove(uuid);
            }
        }
    }

    private String getUuid(IAttributes suite) {
        if (Objects.isNull(suite.getAttribute(ALLURE_UUID))) {
            suite.setAttribute(ALLURE_UUID, UUID.randomUUID().toString());
        }
        return Objects.toString(suite.getAttribute(ALLURE_UUID));
    }

    private String getId(ISuite suite) {
        return getId(suite.getXmlSuite().getName(), suite.getXmlSuite().getParameters());
    }

    private String getId(ITestContext context) {
        return getId(context.getCurrentXmlTest().getName(), context.getCurrentXmlTest().getLocalParameters());
    }

    private String getId(String name, Map<String, String> parameters) {
        MessageDigest digest = getMessageDigest();
        digest.update(name.getBytes(UTF_8));
        parameters.entrySet().stream()
                .map(Map.Entry::getKey)
                .sorted()
                .forEach(key -> digest.update(key.getBytes(UTF_8)));
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

    private void updateTestCase(ITestResult testResult) {
        ITestNGMethod method = testResult.getMethod();
        List<Label> labels = Arrays.asList(
                label("class", method.getTestClass().getName()),
                label("method", method.getMethodName()),
                label("suite", method.getXmlTest().getSuite().getName()),
                label("test", method.getXmlTest().getName()),
                label("host", getHostName().orElse("default")),
                label("thread", getThreadName())
        );

        Allure.LIFECYCLE.updateTestCase(getUuid(testResult),
                t -> t.withId(getId(method.getQualifiedName(), Collections.emptyMap()))
                        .withParentIds(getId(testResult.getTestContext()))
                        .withName(method.getMethodName())
                        .withDescription(method.getDescription())
                        .withLabels(labels)
        );
    }

    private void startBefore(String parentUuid, ITestNGMethod method) {
        String uuid = threadLocalUuid.get();
        TestBeforeResult result = new TestBeforeResult().withName(method.getMethodName());
        Allure.LIFECYCLE.startTestBefore(parentUuid, uuid, result);
    }

    private void startAfter(String parentUuid, ITestNGMethod method) {
        String uuid = threadLocalUuid.get();
        TestAfterResult result = new TestAfterResult().withName(method.getMethodName());
        Allure.LIFECYCLE.startTestAfter(parentUuid, uuid, result);
    }

    private Label label(String name, String value) {
        return new Label().withName(name).withValue(value);
    }

    private List<Parameter> getParameters(ITestResult testResult) {
        String[] parameterNames = Stream.of(testResult.getMethod().getConstructorOrMethod().getMethod().getParameters())
                .map(java.lang.reflect.Parameter::getName)
                .toArray(String[]::new);
        String[] parameterValues = Stream.of(testResult.getParameters())
                .map(Object::toString)
                .toArray(String[]::new);
        return IntStream.range(0, Math.min(parameterNames.length, parameterValues.length))
                .mapToObj(i -> new Parameter().withName(parameterNames[i]).withValue(parameterValues[i]))
                .collect(Collectors.toList());
    }

    private Optional<String> getHostName() {
        try {
            return Optional.of(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            LOGGER.debug("Could not get host name {}", e);
            return Optional.empty();
        }
    }

    private String getThreadName() {
        return String.format("%s.%s(%s)",
                ManagementFactory.getRuntimeMXBean().getName(),
                Thread.currentThread().getName(),
                Thread.currentThread().getId());

    }
}
