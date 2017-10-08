package io.qameta.allure.junit5;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ResultsUtils;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.qameta.allure.model.Status.FAILED;
import static io.qameta.allure.model.Status.PASSED;
import static io.qameta.allure.model.Status.SKIPPED;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author ehborisov
 */
public class AllureJunit5 implements TestExecutionListener {

    private static final String TAG = "tag";


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
            final Optional<MethodSource> methodSource = testIdentifier.getSource()
                    .filter(MethodSource.class::isInstance)
                    .map(MethodSource.class::cast);
            final String uuid = tests.get();
            final TestResult result = new TestResult()
                    .withUuid(uuid)
                    .withName(testIdentifier.getDisplayName())
                    .withLabels(getTags(testIdentifier))
                    .withHistoryId(getHistoryId(testIdentifier))
                    .withStage(Stage.RUNNING);

            methodSource.ifPresent(source -> {
                result.getLabels().add(new Label().withName("suite").withValue(source.getClassName()));
                result.getLabels().add(new Label().withName("package").withValue(source.getClassName()));
            });

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
                            result.setStatus(getStatus(throwable));
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

    protected Status getStatus(final Throwable throwable) {
        return ResultsUtils.getStatus(throwable).orElse(FAILED);
    }

    private List<Label> getTags(final TestIdentifier testIdentifier) {
        return testIdentifier.getTags().stream()
                .map(tag -> new Label().withName(TAG).withValue(tag.getName()))
                .collect(Collectors.toList());
    }

    protected String getHistoryId(final TestIdentifier testIdentifier) {
        return md5(testIdentifier.getUniqueId());
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
