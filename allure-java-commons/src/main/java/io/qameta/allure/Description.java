package io.qameta.allure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Annotation that allows to attach a description for a test or for a step.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Description {

    /**
     * Simple description text as String.
     *
     * @return Description text.
     */
    String value() default "";

    /**
     * Use annotated method's javadoc to extract description that
     * supports html markdown.
     *
     * @return boolean flag to enable description extraction from javadoc.
     */
    boolean useJavaDoc() default false;
}
