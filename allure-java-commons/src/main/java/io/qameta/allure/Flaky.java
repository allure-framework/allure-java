package io.qameta.allure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation to set test suite and test case title.
 * DisplayName overrides default name in report.
 *
 * @author Dmitry Baev charlie@yandex-team.ru
 *         Date: 10.24.13
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Flaky {
}