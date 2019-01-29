package io.qameta.allure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation. Annotations marked by this annotation will be discovered
 * by Allure and added to test results as a label.
 *
 * @see Epic
 * @see Feature
 * @see Story
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
@Repeatable(LabelAnnotations.class)
public @interface LabelAnnotation {

    String DEFAULT_VALUE = "$$$$$$$$__value__$$$$$$$$";

    /**
     * The name of label. Some build-in names can be
     * found in {@link io.qameta.allure.util.ResultsUtils}. You can
     * also use any custom label name and create mapping for it in
     * Allure Enterprise or Allure 3.
     *
     * @return the name of label to add.
     */
    String name();

    /**
     * Th value of label. In not specified will take value from <code>value()</code>
     * method of target annotation.
     *
     * @return the value of label to add.
     */
    String value() default DEFAULT_VALUE;

}
