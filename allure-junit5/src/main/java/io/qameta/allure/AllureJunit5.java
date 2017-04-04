package io.qameta.allure;

import io.qameta.allure.model.Stage;
import io.qameta.allure.model.TestResult;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static io.qameta.allure.model.Status.FAILED;
import static io.qameta.allure.model.Status.PASSED;
import static io.qameta.allure.model.Status.SKIPPED;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author ehborisov
 */
public class AllureJunit5 implements TestExecutionListener {

    private final ThreadLocal<String> tests
            = InheritableThreadLocal.withInitial(() -> UUID.randomUUID().toString());

    private final AllureLifecycle lifecycle;

    public AllureJunit5(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public AllureJunit5() {
        this.lifecycle = Allure.getLifecycle();
    }

    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }

    @Override
    public void executionStarted(final TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            final String uuid = tests.get();
            final TestResult result = new TestResult()
                    .withUuid(uuid)
                    .withName(testIdentifier.getDisplayName())
                    .withHistoryId(md5(testIdentifier.getUniqueId()))
                    .withStage(Stage.RUNNING);
            getLifecycle().scheduleTestCase(result);
            getLifecycle().startTestCase(uuid);
        }
    }

    @Override
    public void executionFinished(final TestIdentifier testIdentifier, final TestExecutionResult testExecutionResult) {
        if (testIdentifier.isTest()) {
            final String uuid = tests.get();
            tests.remove();
            getLifecycle().updateTestCase(uuid, result -> {
                result.setStage(Stage.FINISHED);
                switch (testExecutionResult.getStatus()) {
                    case FAILED:
                        testExecutionResult.getThrowable().ifPresent(throwable -> {
                            result.setStatus(ResultsUtils.getStatus(throwable).orElse(FAILED));
                            result.setStatusDetails(ResultsUtils.getStatusDetails(throwable).orElse(null));
                        });
                        break;
                    case SUCCESSFUL:
                        result.setStatus(PASSED);
                        break;
                    default:
                        result.setStatus(SKIPPED);
                        testExecutionResult.getThrowable().ifPresent(throwable ->
                                result.setStatusDetails(ResultsUtils.getStatusDetails(throwable).orElse(null))
                        );
                        break;
                }
            });
            getLifecycle().stopTestCase(uuid);
            getLifecycle().writeTestCase(uuid);
        }
    }

    private String md5(final String source) {
        final byte[] bytes = getMessageDigest().digest(source.getBytes(UTF_8));
        return new BigInteger(1, bytes).toString(16);
    }

    private MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find md5 hashing algorithm", e);
        }
    }
}
