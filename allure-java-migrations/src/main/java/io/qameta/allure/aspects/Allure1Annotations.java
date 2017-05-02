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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Allure labels utils.
 */
final class Allure1Annotations {

    private static final String SUITE_LABEL = "suite";

    private final MethodSignature signature;

    private final Object target;

    private final Object[] args;


    Allure1Annotations(final Object target, final MethodSignature signature, final Object... args) {
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

        return fields.stream().collect(
                Collectors.toMap(Allure1Utils::getParameterName, f -> Allure1Utils.getParameterValue(f, target))
        );
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
        labels.addAll(Allure1Utils.getLabels(method, TestCaseId.class, Allure1Utils::createLabels));
        labels.addAll(Allure1Utils.getLabels(method, Issue.class, Allure1Utils::createLabels));
        labels.addAll(Allure1Utils.getLabels(method, Issues.class, Allure1Utils::createLabels));
        labels.addAll(Allure1Utils.getLabels(method, Stories.class, Allure1Utils::createLabels));
        labels.addAll(Allure1Utils.getLabels(method, Features.class, Allure1Utils::createLabels));
        return labels;
    }

}
