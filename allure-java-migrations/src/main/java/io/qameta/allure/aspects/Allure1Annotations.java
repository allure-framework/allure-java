package io.qameta.allure.aspects;

import io.qameta.allure.model.Label;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.TestResult;
import org.apache.commons.lang3.reflect.FieldUtils;
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final String SUITE_LABEL = "suite";

    private final MethodSignature signature;

    private final Object target;

    private final Object[] args;


    public Allure1Annotations(final Object target, final MethodSignature signature, final Object... args) {
        this.args = Arrays.copyOf(args, args.length);
        this.signature = signature;
        this.target = target;
    }

    public void updateTitle(final TestResult result) {
        final Method method = getMethod();
        if (method.isAnnotationPresent(Title.class)) {
            final Title title = method.getAnnotation(Title.class);
            result.setName(title.value());
        }
        final Class<?> type = getType();
        if (type.isAnnotationPresent(Title.class)) {
            final Title title = type.getAnnotation(Title.class);
            final List<Label> labels = result.getLabels().stream()
                    .filter(label -> !label.getName().equals(SUITE_LABEL))
                    .collect(Collectors.toList());
            labels.add(new Label().withName(SUITE_LABEL).withValue(title.value()));
            result.setLabels(labels);
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

    public void updateLabels(final TestResult result) {
        result.getLabels().addAll(getLabels());
    }

    public void updateParameters(final TestResult result) {
        final Map<String, String> parameters = getParameters();
        result.getParameters().stream()
                .map(Parameter::getName)
                .filter(parameters::containsKey)
                .forEach(parameters::remove);
        parameters.forEach((n, v) -> result.getParameters().add(new Parameter().withName(n).withValue(v)));
    }

    private Map<String, String> getParameters() {
        final Map<String, String> parameters = new HashMap<>();
        parameters.putAll(getMethodParameters());
        parameters.putAll(getClassParameters());
        return parameters;
    }

    private Map<String, String> getMethodParameters() {
        final Map<String, String> parameters = new HashMap<>();
        final String[] names = signature.getParameterNames();
        for (int i = 0; i < names.length; i++) {
            parameters.put(names[i], args[i].toString());
        }
        return parameters;
    }

    private Map<String, String> getClassParameters() {
        final List<Field> fields = FieldUtils.getFieldsListWithAnnotation(getType(),
                ru.yandex.qatools.allure.annotations.Parameter.class);
        return fields.stream().collect(Collectors.toMap(f -> getParameterName(f), f -> getParameterValue(f, target)));
    }

    private Class<?> getType() {
        return signature.getMethod().getDeclaringClass();
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

    private static String getParameterName(final Field field) {
        final String value = field.getAnnotation(ru.yandex.qatools.allure.annotations.Parameter.class).value();
        return value.isEmpty() ? field.getName() : value;
    }

    private static String getParameterValue(final Field field, final Object target) {
        try {
            return field.get(target).toString();
        } catch (IllegalAccessException e) {
            return null;
        }
    }
}
