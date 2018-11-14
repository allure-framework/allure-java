package io.qameta.allure.test;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.aspects.AttachmentsAspects;
import io.qameta.allure.aspects.StepsAspects;
import io.qameta.allure.model.TestResult;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;

/**
 * @author charlie (Dmitry Baev).
 */
public final class RunUtils {

    private RunUtils() {
        throw new IllegalStateException("do not instance");
    }

    public static AllureResults runWithinTestContext(final Runnable runnable) {
        return runWithinTestContext(
                runnable,
                Allure::setLifecycle,
                StepsAspects::setLifecycle,
                AttachmentsAspects::setLifecycle
        );
    }

    @SafeVarargs
    public static AllureResults runWithinTestContext(final Runnable runnable,
                                                     final Consumer<AllureLifecycle>... configurers) {
        final AllureResultsWriterStub writer = new AllureResultsWriterStub();
        final AllureLifecycle lifecycle = new AllureLifecycle(writer);

        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().setUuid(uuid);

        final AllureLifecycle cached = Allure.getLifecycle();
        try {
            Stream.of(configurers).forEach(configurer -> configurer.accept(lifecycle));

            lifecycle.scheduleTestCase(result);
            lifecycle.startTestCase(uuid);

            runnable.run();
        } catch (Throwable e) {
            lifecycle.updateTestCase(uuid, testResult -> {
                getStatus(e).ifPresent(testResult::setStatus);
                getStatusDetails(e).ifPresent(testResult::setStatusDetails);

            });
        } finally {
            lifecycle.stopTestCase(uuid);
            lifecycle.writeTestCase(uuid);

            Stream.of(configurers).forEach(configurer -> configurer.accept(cached));
        }

        return writer;
    }

}
