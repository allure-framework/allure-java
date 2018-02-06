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

    /**
     * This flag adds <b>&#60;br /&#62;</b> tag after each line of javadoc description.
     * Works only with {@link io.qameta.allure.Description#useJavaDoc()} enabled.
     *
     * @return boolean flag to enable line separation
     */
    boolean separateLines() default false;
}
