package ru.yandex.qatools.allure.annotations;

import ru.yandex.qatools.allure.model.SeverityLevel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Each test has severity level. You can change test case severity
 * label using this annotation.
 *
 * @see ru.yandex.qatools.allure.model.SeverityLevel
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Severity {

    SeverityLevel value();

}
