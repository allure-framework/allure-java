package io.qameta.allure.junit4;

import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureJunit4 extends RunListener {

    public static final String MD_5 = "md5";

    private final ThreadLocal<String> testCases
            = InheritableThreadLocal.withInitial(() -> UUID.randomUUID().toString());

    private final Map<String, String> suites = new HashMap<>();

    @Override
    public void testRunStarted(Description description) throws Exception {
        createTestGroups(null, description.getChildren());
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
    }

    @Override
    public void testStarted(Description description) throws Exception {
        String uuid = testCases.get();
        String id = getHistoryId(description);
        String testGroupId = getSuiteUuid(description.getClassName());

        AllureLifecycle.INSTANCE.scheduleTestCase(testGroupId, new TestResult()
                .withUuid(uuid)
                .withHistoryId(id)
                .withName(description.getMethodName())
                .withStart(System.currentTimeMillis())
                .withLabels(
                        label("package", getPackage(description.getTestClass())),
                        label("testMethod", description.getMethodName()),
                        label("testClass", description.getClassName())
                )
        );
    }

    @Override
    public void testFinished(Description description) throws Exception {
//        AllureOld.INSTANCE.closeTestCase();
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
//        AllureOld.INSTANCE.setTestCaseStatus(of(failure.getException()));
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
    }

    @Override
    public void testIgnored(Description description) throws Exception {
    }

    private void createTestGroups(String parentUuid, List<Description> descriptions) {
        descriptions.forEach(description -> {
            String uuid = getSuiteUuid(description.getClassName());
            AllureLifecycle.INSTANCE.startTestContainer(parentUuid, new TestResultContainer()
                    .withUuid(uuid)
                    .withName(description.getDisplayName())
            );
            AllureLifecycle.INSTANCE.writeTestContainer(uuid);

            description.getChildren().forEach(child ->
                    createTestGroups(uuid, description.getChildren())
            );
        });
    }

    private String getHistoryId(Description description) {
        return md5(description.getClassName() + description.getMethodName());
    }

    private String getSuiteUuid(String className) {
        return suites.computeIfAbsent(className, this::md5);
    }

    private String md5(String source) {
        byte[] bytes = getMessageDigest().digest(source.getBytes(UTF_8));
        return new BigInteger(1, bytes).toString(16);
    }

    private MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance(MD_5);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find md5 hashing algorithm");
        }
    }

    private Label label(String name, String value) {
        return new Label().withName(name).withValue(value);
    }

    private String getPackage(Class<?> testClass) {
        return testClass.getPackage().getName();
    }
}
