package io.qameta.allure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotations to mark step methods.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Step {

    /**
     * The step text.
     *
     * @return the step text.
     */
    String value() default "";

    /**
     * Option for hide step parameters.
     *
     * @return boolean flag to disable step parameters showing.
     */
    boolean hideParams() default false;

}
