package io.qameta.allure.test;

import io.qameta.allure.model.Label;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestResult;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings({"PMD.ClassNamingConventions", "PMD.LinguisticNaming"})
public final class AllurePredicates {

    private AllurePredicates() {
        throw new IllegalStateException("Do not instance");
    }

    public static Predicate<TestResult> hasStatus(final Status status) {
        return testResult -> status.equals(testResult.getStatus());
    }

    public static Predicate<TestResult> hasLabel(final String name, final String value) {
        final Predicate<Label> labelPredicate = label -> Objects.equals(label.getName(), name)
                && Objects.equals(label.getValue(), value);

        return testResult -> testResult.getLabels().stream().anyMatch(labelPredicate);
    }
}
