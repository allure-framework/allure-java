package io.qameta.allure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static io.qameta.allure.util.ResultsUtils.EPIC_LABEL_NAME;

/**
 * Used to mark tests with epic label.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Repeatable(Epics.class)
@LabelAnnotation(name = EPIC_LABEL_NAME)
public @interface Epic {

    String value();

}
