package io.qameta.allure.spock;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ResultsUtils;
import org.spockframework.runtime.AbstractRunListener;
import org.spockframework.runtime.extension.IGlobalExtension;
import org.spockframework.runtime.model.ErrorInfo;
import org.spockframework.runtime.model.FeatureInfo;
import org.spockframework.runtime.model.IterationInfo;
import org.spockframework.runtime.model.SpecInfo;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.qameta.allure.util.ResultsUtils.getStatusDetails;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureSpock extends AbstractRunListener implements IGlobalExtension {

    private final ThreadLocal<String> testResults
            = InheritableThreadLocal.withInitial(() -> UUID.randomUUID().toString());

    private AllureLifecycle lifecycle;

    public AllureSpock() {
        this(Allure.getLifecycle());
    }

    public AllureSpock(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public void start() {
        //do nothing at this point
    }

    @Override
    public void visitSpec(final SpecInfo spec) {
        spec.addListener(this);
    }

    @Override
    public void stop() {
        //do nothing at this point
    }

    @Override
    public void beforeIteration(final IterationInfo iteration) {
        final String uuid = testResults.get();
        final FeatureInfo feature = iteration.getFeature();
        final SpecInfo spec = feature.getSpec();
        final List<Parameter> parameters = getParameters(feature.getDataVariables(), iteration.getDataValues());

        final TestResult result = new TestResult()
                .withUuid(uuid)
                .withName(iteration.getName())
                .withFullName(String.format("%s.%s", spec.getPackage(), feature.getName()))
                .withParameters(parameters)
                .withLabels(
                        new Label().withName("feature").withValue(spec.getName()),
                        new Label().withName("story").withValue(feature.getName()),
                        new Label().withName("suite").withValue(spec.getName()),
                        new Label().withName("subSuite").withValue(feature.getName()),
                        new Label().withName("package").withValue(spec.getPackage())
                );

        getLifecycle().scheduleTestCase(result);
        getLifecycle().startTestCase(uuid);
    }

    @Override
    public void error(final ErrorInfo error) {
        final String uuid = testResults.get();
        final Status status = getStatus(error.getException());
        final StatusDetails details = getStatusDetails(error.getException()).orElse(null);

        getLifecycle().updateTestCase(uuid, testResult -> testResult
                .withStatus(status)
                .withStatusDetails(details)
        );
    }

    @Override
    public void afterIteration(final IterationInfo iteration) {
        final String uuid = testResults.get();
        testResults.remove();

        getLifecycle().updateTestCase(uuid, testResult -> {
            if (Objects.isNull(testResult.getStatus())) {
                testResult.setStatus(Status.PASSED);
            }
        });
        getLifecycle().stopTestCase(uuid);
        getLifecycle().writeTestCase(uuid);
    }

    protected Status getStatus(final Throwable throwable) {
        return ResultsUtils.getStatus(throwable).orElse(Status.BROKEN);
    }

    private List<Parameter> getParameters(final List<String> names, final Object... values) {
        return IntStream.range(0, Math.min(names.size(), values.length))
                .mapToObj(index -> new Parameter()
                        .withName(names.get(index))
                        .withValue(Objects.toString(values[index])))
                .collect(Collectors.toList());
    }

    public AllureLifecycle getLifecycle() {
        return lifecycle;
    }
}
