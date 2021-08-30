package io.qameta.allure.hamcrest;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.util.ObjectUtils;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;

import java.util.UUID;

import static io.qameta.allure.util.ResultsUtils.getStatus;

/**
 * <p>
 * Aspect "interceptor" for automatic logging to the Allure report.
 * </p>
 * <p>
 * This aspect should work for all asserts that are in the Hamcrest library, since they all go through a single method
 * to start the comparison.
 * </p>
 * <p>
 * In addition to the standard comparisons that are already in the Hamcrest library, this aspect should work correctly
 * with custom matchers if developers correctly implemented the describeMismatch and / or describeMismatchSafely
 * methods in the TypeSafeMatcher class.
 * </p>
 *
 * @author a-simeshin (Simeshin Artem)
 * @see org.hamcrest.TypeSafeMatcher
 */
@Aspect
@SuppressWarnings("all")
public class AllureHamcrestAssert {

    private static InheritableThreadLocal<AllureLifecycle> lifecycle = new InheritableThreadLocal<AllureLifecycle>() {
        @Override
        protected AllureLifecycle initialValue() {
            return Allure.getLifecycle();
        }
    };

    public static AllureLifecycle getLifecycle() {
        return lifecycle.get();
    }

    @Pointcut("execution(void org.hamcrest.MatcherAssert.**(..))")
    public void initAssertThat() {
    }

    /**
     * <p>
     * assertThat(String comment, Object actual, Matcher expected) - one and only one central entry point for all
     * asserts. Based on this rule, you only need to log a method with three arguments.
     * </p>
     * <p>
     * Even if there is no comment as the first argument, an empty string will be passed as the first argument.
     * </p>
     * <p>
     * For example assertThat(value123.get(), is(equalTo("value123"))) will be proxied to the metod
     * assertThat("", value123.get(), is(equalTo("value123")))
     * </p>
     *
     * @param joinPoint - entry point with args and method name
     */
    @Before("initAssertThat()")
    public void catchAndStartStep(final JoinPoint joinPoint) {
        if (joinPoint.getArgs().length == 3) {
            final String reason = (String) joinPoint.getArgs()[0];
            final String actual = ObjectUtils.toString(joinPoint.getArgs()[1]);
            final StringDescription description = new StringDescription();
            final String expecting = description.appendText("assert \"")
                    .appendText(actual)
                    .appendText("\" ")
                    .appendDescriptionOf((Matcher) joinPoint.getArgs()[2])
                    .toString();

            getLifecycle().startStep(
                    UUID.randomUUID().toString(),
                    new StepResult()
                            .setName(reason.isEmpty() ? expecting : expecting + " | " + reason)
                            .setDescription("Hamcrest assert")
                            .setStatus(Status.PASSED)
            );
        }
    }

    @AfterThrowing(pointcut = "initAssertThat()", throwing = "e")
    public void stepFailed(final Throwable e) {
        getLifecycle().updateStep(s -> s.setStatus(getStatus(e).orElse(Status.BROKEN)));
        getLifecycle().stopStep();
    }

    @AfterReturning(pointcut = "initAssertThat()")
    public void stepStop() {
        getLifecycle().updateStep(s -> s.setStatus(Status.PASSED));
        getLifecycle().stopStep();
    }

    /**
     * For tests only.
     *
     * @param allure allure lifecycle to set
     */
    public static void setLifecycle(final AllureLifecycle allure) {
        lifecycle.set(allure);
    }
}