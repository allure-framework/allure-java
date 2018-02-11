package io.qameta.allure.junit4;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

import java.lang.annotation.Annotation;

public class SampleRunnerBasedOnNotClasses extends Runner {
    @SuppressWarnings("unused")
    public SampleRunnerBasedOnNotClasses(Class testClass) {
        super();
    }

    @Override
    public Description getDescription() {
        return Description.createTestDescription(
            "allure junit4 runner.test for non-existing classes (would be a class in normal runner)",
            "should correctly handle non-existing classes (would be method name in normal runner)",
            new DisplayName() {
                @Override
                public String value() {
                    return "Some human readable name";
                }

                @Override
                public Class<? extends Annotation> annotationType() {
                    return DisplayName.class;
                }
            }
        );
    }

    @Override
    public void run(RunNotifier notifier) {
        notifier.fireTestStarted(getDescription());
        notifier.fireTestFinished(getDescription());
    }
}
