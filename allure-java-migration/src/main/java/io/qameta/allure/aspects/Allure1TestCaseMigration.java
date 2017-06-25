package io.qameta.allure.aspects;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.Objects;

/**
 * A base class for test case migration. Should be subclassed in order to
 * provide migration from a concrete testing framework.
 */
public class Allure1TestCaseMigration {

    private static AllureLifecycle lifecycle;

    protected void updateTestCase(final JoinPoint joinPoint) {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final Object[] args = joinPoint.getArgs();
        final Object target = joinPoint.getTarget();
        final Allure1Annotations annotations = new Allure1Annotations(target, signature, args);
        getLifecycle().getCurrentTestCase().ifPresent(uuid -> {
            getLifecycle().updateTestCase(uuid, annotations::updateTitle);
            getLifecycle().updateTestCase(uuid, annotations::updateDescription);
            getLifecycle().updateTestCase(uuid, annotations::updateParameters);
            getLifecycle().updateTestCase(uuid, annotations::updateLabels);
        });
    }

    /**
     * For tests only.
     */
    public static void setLifecycle(final AllureLifecycle lifecycle) {
        Allure1TestCaseMigration.lifecycle = lifecycle;
    }

    public static AllureLifecycle getLifecycle() {
        if (Objects.isNull(lifecycle)) {
            lifecycle = Allure.getLifecycle();
        }
        return lifecycle;
    }

}
