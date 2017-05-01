package io.qameta.allure.aspects;

import io.qameta.allure.model.Label;
import io.qameta.allure.model.TestResult;
import org.aspectj.lang.reflect.MethodSignature;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Severity;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.allure.model.DescriptionType;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Allure labels utils.
 */
public final class Allure1Annotations {

    private static final String SEVERITY_LABEL = "severity";

    private static final String FEATURE_LABEL = "feature";

    private static final String STORY_LABEL = "story";

    private static final String ISSUE_LABEL = "issue";

    private final MethodSignature signature;

    public Allure1Annotations(final MethodSignature signature) {
        this.signature = signature;
    }

    public void updateTitle(final TestResult result) {
        final Method method = getMethod();
        if (method.isAnnotationPresent(Title.class)) {
            final Title title = getMethod().getAnnotation(Title.class);
            result.setName(title.value());
        }
    }

    public void updateDescription(final TestResult result) {
        final Method method = getMethod();
        if (method.isAnnotationPresent(Description.class)) {
            final Description description = method.getAnnotation(Description.class);
            if (description.type().equals(DescriptionType.HTML)) {
                result.setDescriptionHtml(description.value());
            } else {
                result.setDescription(description.value());
            }
        }
    }

    @SuppressWarnings("PMD")
    public void updateParameters(final TestResult result) {

    }

    public void updateLabels(final TestResult result) {
        result.getLabels().addAll(getLabels());
    }

    private Method getMethod() {
        return signature.getMethod();
    }

    private List<Label> getLabels() {
        final Method method = getMethod();
        final List<Label> labels = new ArrayList<>();
        labels.addAll(getLabels(method, Severity.class, Allure1Annotations::createLabels));
        labels.addAll(getLabels(method, TestCaseId.class, Allure1Annotations::createLabels));
        labels.addAll(getLabels(method, Issue.class, Allure1Annotations::createLabels));
        labels.addAll(getLabels(method, Issues.class, Allure1Annotations::createLabels));
        labels.addAll(getLabels(method, Stories.class, Allure1Annotations::createLabels));
        labels.addAll(getLabels(method, Features.class, Allure1Annotations::createLabels));
        return labels;
    }

    private static <T extends Annotation> List<Label> getLabels(final Method method, final Class<T> annotation,
                                                                final Function<T, List<Label>> extractor) {
        final List<Label> labels = new ArrayList<>();
        labels.addAll(getLabels((AnnotatedElement) method, annotation, extractor));
        labels.addAll(getLabels(method.getDeclaringClass(), annotation, extractor));
        return labels;
    }

    private static <T extends Annotation> List<Label> getLabels(final AnnotatedElement element,
                                                                final Class<T> annotation,
                                                                final Function<T, List<Label>> extractor) {
        return element.isAnnotationPresent(annotation)
                ? extractor.apply(element.getAnnotation(annotation))
                : Collections.emptyList();
    }

    private static List<Label> createLabels(final Stories stories) {
        return Arrays.stream(stories.value())
                .map(value -> new Label().withName(STORY_LABEL).withValue(value))
                .collect(Collectors.toList());
    }

    private static List<Label> createLabels(final Features features) {
        return Arrays.stream(features.value())
                .map(value -> new Label().withName(FEATURE_LABEL).withValue(value))
                .collect(Collectors.toList());
    }

    private static List<Label> createLabels(final Severity severity) {
        return Collections.singletonList(new Label().withName(SEVERITY_LABEL).withValue(severity.value().value()));
    }

    private static List<Label> createLabels(final Issues issues) {
        return Arrays.stream(issues.value())
                .map(Allure1Annotations::createLabel)
                .collect(Collectors.toList());
    }

    private static List<Label> createLabels(final Issue issue) {
        return Collections.singletonList(new Label().withName(ISSUE_LABEL).withValue(issue.value()));
    }

    private static List<Label> createLabels(final TestCaseId issue) {
        return Collections.singletonList(new Label().withName(ISSUE_LABEL).withValue(issue.value()));
    }

    private static Label createLabel(final Issue issue) {
        return new Label().withName(ISSUE_LABEL).withValue(issue.value());
    }

}
