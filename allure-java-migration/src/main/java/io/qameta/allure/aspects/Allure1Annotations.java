package io.qameta.allure.aspects;

import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Allure labels utils.
 */
final class Allure1Annotations {

    private static final String SUITE_LABEL = "suite";

    private final MethodSignature signature;

    private final Object[] args;

    Allure1Annotations(final MethodSignature signature, final Object... args) {
        this.args = Arrays.copyOf(args, args.length);
        this.signature = signature;
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
            labels.add(new Label().setName(SUITE_LABEL).setValue(title.value()));
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

    public void updateLinks(final TestResult result) {
        result.getLinks().addAll(getLinks());
    }

    public void updateParameters(final TestResult result) {
        result.getParameters().addAll(getMethodParameters());
    }

    private List<Parameter> getMethodParameters() {
        final String[] parameterNames = signature.getParameterNames();
        return IntStream.range(0, parameterNames.length)
                .mapToObj(index -> {
                    final String name = parameterNames[index];
                    final String value = Objects.toString(args[index]);
                    return new Parameter()
                            .setName(name)
                            .setValue(value);
                })
                .collect(Collectors.toList());
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
        labels.addAll(Allure1Utils.getLabels(method, Severity.class, Allure1Utils::createLabels));
        labels.addAll(Allure1Utils.getLabels(method, Stories.class, Allure1Utils::createLabels));
        labels.addAll(Allure1Utils.getLabels(method, Features.class, Allure1Utils::createLabels));
        return labels;
    }

    public List<Link> getLinks() {
        final Method method = getMethod();
        final List<Link> links = new ArrayList<>();
        links.addAll(Allure1Utils.getLinks(method, TestCaseId.class, Allure1Utils::createLinks));
        links.addAll(Allure1Utils.getLinks(method, Issue.class, Allure1Utils::createLinks));
        links.addAll(Allure1Utils.getLinks(method, Issues.class, Allure1Utils::createLinks));
        return links;
    }
}
