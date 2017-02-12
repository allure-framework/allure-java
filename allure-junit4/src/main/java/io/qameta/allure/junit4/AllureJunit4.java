package io.qameta.allure.junit4;

import com.google.common.hash.Hashing;
import io.qameta.allure.Allure;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.TestCaseResult;
import io.qameta.allure.model.TestGroupResult;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureJunit4 extends RunListener {

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
        String id = getCaseId(description);
        String testGroupId = getSuiteId(description.getClassName());
        Allure.LIFECYCLE.scheduleTestCase(uuid, new TestCaseResult()
                .withId(id)
                .withParentIds(testGroupId)
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
//        AllureOld.LIFECYCLE.closeTestCase();
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
//        AllureOld.LIFECYCLE.setTestCaseStatus(of(failure.getException()));
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
    }

    @Override
    public void testIgnored(Description description) throws Exception {
    }

    private void createTestGroups(String parentId, List<Description> descriptions) {
        descriptions.forEach(description -> {
            String id = getSuiteId(description.getClassName());
            Allure.LIFECYCLE.startTestGroup(id, new TestGroupResult()
                    .withId(id)
                    .withParentIds(Objects.isNull(parentId) ? emptyList() : singletonList(parentId))
                    .withName(description.getDisplayName())
                    .withType("suite")
            );
            Allure.LIFECYCLE.stopTestGroup(id);

            description.getChildren().forEach(child ->
                    createTestGroups(id, description.getChildren())
            );
        });
    }

    private String getCaseId(Description description) {
        return md5(description.getClassName() + description.getMethodName());
    }

    private String getSuiteId(String className) {
        return suites.computeIfAbsent(className, this::md5);
    }

    private String md5(String source) {
        byte[] bytes = Hashing.md5()
                .hashString(source, StandardCharsets.UTF_8)
                .asBytes();
        return new BigInteger(1, bytes).toString(16);
    }

    private Label label(String name, String value) {
        return new Label().withName(name).withValue(value);
    }

    private String getPackage(Class<?> testClass) {
        return testClass.getPackage().getName();
    }
}
