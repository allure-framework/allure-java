package io.qameta.allure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation to add some links to results. Usage:
 * <pre>
 * &#064;Link("https://qameta.io")
 * public void myTest() {
 *     ...
 * }
 * </pre>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Repeatable(Links.class)
public @interface Link {

    /**
     * Alias for {@link #name()}.
     *
     * @return the link name.
     */
    String value() default "";

    /**
     * Name for link, by default url.
     *
     * @return the link name.
     */
    String name() default "";

    /**
     * Url for link. By default will search for system property `allure.link.{type}.pattern`, and use it
     * to generate url.
     *
     * @return the link url.
     */
    String url() default "";

    /**
     * This type is used for create an icon for link. Also there is few reserved types such as issue and tms.
     *
     * @return the link type.
     */
    String type() default "custom";
}
