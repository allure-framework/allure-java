package io.qameta.allure.spock;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Flaky;
import io.qameta.allure.Issue;
import io.qameta.allure.Link;
import io.qameta.allure.Muted;
import io.qameta.allure.Owner;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import io.qameta.allure.model.ExecutableItem;
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

import java.lang.annotation.Annotation;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.firstNonEmpty;
import static io.qameta.allure.util.ResultsUtils.getHostName;
import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;
import static io.qameta.allure.util.ResultsUtils.getThreadName;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparing;


/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings("PMD.UnnecessaryFullyQualifiedName")
public class AllureSpock extends AbstractRunListener implements IGlobalExtension {

    private static final String MD_5 = "md5";

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
        final SpecInfo subSpec = spec.getSubSpec();
        final SpecInfo superSpec = spec.getSuperSpec();
        final String packageName = spec.getPackage();
        final String specName = spec.getName();
        final String testClassName = feature.getDescription().getClassName();
        final String testMethodName = iteration.getName();

        final List<Label> labels = new ArrayList<>();
        labels.addAll(Arrays.asList(
                //Packages grouping
                new Label().withName("package").withValue(packageName),
                new Label().withName("testClass").withValue(testClassName),
                new Label().withName("testMethod").withValue(testMethodName),
                //xUnit grouping
                new Label().withName("suite").withValue(specName),
                //Timeline grouping
                new Label().withName("host").withValue(getHostName()),
                new Label().withName("thread").withValue(getThreadName())
        ));
        if (Objects.nonNull(subSpec)) {
            labels.add(new Label().withName("subSuite").withValue(subSpec.getName()));
        }
        if (Objects.nonNull(superSpec)) {
            labels.add(new Label().withName("parentSuite").withValue(superSpec.getName()));
        }
        labels.addAll(getLabels(iteration));

        final TestResult result = new TestResult()
                .withUuid(uuid)
                .withHistoryId(getHistoryId(getQualifiedName(iteration), parameters))
                .withName(firstNonEmpty(
                        testMethodName,
                        feature.getDescription().getDisplayName(),
                        getQualifiedName(iteration)).orElse("Unknown"))
                .withFullName(getQualifiedName(iteration))
                .withStatusDetails(new StatusDetails()
                        .withFlaky(isFlaky(iteration))
                        .withMuted(isMuted(iteration)))
                .withParameters(parameters)
                .withLinks(getLinks(iteration))
                .withLabels(labels);
        processDescription(iteration, result);
        getLifecycle().scheduleTestCase(result);
        getLifecycle().startTestCase(uuid);
    }

    private List<Label> getLabels(final IterationInfo iterationInfo) {
        return Stream.of(
                getLabels(iterationInfo, Epic.class, ResultsUtils::createLabel),
                getLabels(iterationInfo, Feature.class, ResultsUtils::createLabel),
                getLabels(iterationInfo, Story.class, ResultsUtils::createLabel),
                getLabels(iterationInfo, Severity.class, ResultsUtils::createLabel),
                getLabels(iterationInfo, Owner.class, ResultsUtils::createLabel)
        ).reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toList());
    }

    private <T extends Annotation> Stream<Label> getLabels(final IterationInfo iterationInfo, final Class<T> clazz,
                                                           final Function<T, Label> extractor) {
        final List<Label> onFeature = getFeatureAnnotations(iterationInfo, clazz).stream()
                .map(extractor)
                .collect(Collectors.toList());
        if (!onFeature.isEmpty()) {
            return onFeature.stream();
        }
        return getSpecAnnotations(iterationInfo, clazz).stream()
                .map(extractor);
    }

    private void processDescription(final IterationInfo iterationInfo, final ExecutableItem item) {
        final List<Description> annotationsOnFeature = getFeatureAnnotations(iterationInfo, Description.class);
        if (!annotationsOnFeature.isEmpty()) {
            item.withDescription(annotationsOnFeature.get(0).value());
        }
    }

    private String getQualifiedName(final IterationInfo iteration) {
        return iteration.getDescription().getClassName() + "." + iteration.getName();
    }

    private String getHistoryId(final String name, final List<Parameter> parameters) {
        final MessageDigest digest = getMessageDigest();
        digest.update(name.getBytes(UTF_8));
        parameters.stream()
                .sorted(comparing(Parameter::getName).thenComparing(Parameter::getValue))
                .forEachOrdered(parameter -> {
                    digest.update(parameter.getName().getBytes(UTF_8));
                    digest.update(parameter.getValue().getBytes(UTF_8));
                });
        final byte[] bytes = digest.digest();
        return new BigInteger(1, bytes).toString(16);
    }

    private MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance(MD_5);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find md5 hashing algorithm", e);
        }
    }

    private boolean isFlaky(final IterationInfo iteration) {
        return hasAnnotation(iteration, Flaky.class);
    }

    private boolean isMuted(final IterationInfo iteration) {
        return hasAnnotation(iteration, Muted.class);
    }

    private boolean hasAnnotation(final IterationInfo iteration, final Class<? extends Annotation> clazz) {
        return hasAnnotationOnFeature(iteration, clazz) || hasAnnotationOnSpec(iteration, clazz);
    }

    private boolean hasAnnotationOnSpec(final IterationInfo iteration, final Class<? extends Annotation> clazz) {
        return !getSpecAnnotations(iteration, clazz).isEmpty();
    }

    private boolean hasAnnotationOnFeature(final IterationInfo iteration, final Class<? extends Annotation> clazz) {
        return !getFeatureAnnotations(iteration, clazz).isEmpty();
    }

    private List<io.qameta.allure.model.Link> getLinks(final IterationInfo iteration) {
        return Stream.of(
                getSpecAnnotations(iteration, Link.class).stream().map(ResultsUtils::createLink),
                getFeatureAnnotations(iteration, Link.class).stream().map(ResultsUtils::createLink),
                getSpecAnnotations(iteration, Issue.class).stream().map(ResultsUtils::createLink),
                getFeatureAnnotations(iteration, Issue.class).stream().map(ResultsUtils::createLink),
                getSpecAnnotations(iteration, TmsLink.class).stream().map(ResultsUtils::createLink),
                getFeatureAnnotations(iteration, TmsLink.class).stream().map(ResultsUtils::createLink)
        ).reduce(Stream::concat).orElseGet(Stream::empty).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private <T extends Annotation> List<T> getFeatureAnnotations(final IterationInfo iteration, final Class<T> clazz) {
        final List<T> filteredAnnotations = new ArrayList<>();
        final Collection<Annotation> annotationCollection = iteration.getFeature().getDescription().getAnnotations();
        annotationCollection.forEach(annotation -> {
            if (annotation.annotationType() == clazz) {
                filteredAnnotations.add((T) annotation);
            }
        });
        return filteredAnnotations;
    }

    @SuppressWarnings("unchecked")
    private <T extends Annotation> List<T> getSpecAnnotations(final IterationInfo iteration, final Class<T> clazz) {
        final List<T> filteredAnnotations = new ArrayList<>();
        final SpecInfo spec = iteration.getFeature().getSpec();
        final Collection<Annotation> annotationCollection = spec.getDescription().getAnnotations();
        annotationCollection.forEach(annotation -> {
            if (annotation.annotationType() == clazz) {
                filteredAnnotations.add((T) annotation);
            }
        });
        return filteredAnnotations;
    }

    @Override
    public void error(final ErrorInfo error) {
        final String uuid = testResults.get();
        getLifecycle().updateTestCase(uuid, testResult -> testResult
                .withStatus(getStatus(error.getException()).orElse(null))
                .withStatusDetails(getStatusDetails(error.getException()).orElse(null))
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
