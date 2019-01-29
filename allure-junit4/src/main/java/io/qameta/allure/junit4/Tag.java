package io.qameta.allure.junit4;

import io.qameta.allure.LabelAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static io.qameta.allure.util.ResultsUtils.TAG_LABEL_NAME;

/**
 * @author jkttt on 05.07.17.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(Tags.class)
@LabelAnnotation(name = TAG_LABEL_NAME)
public @interface Tag {

    String value();

}
