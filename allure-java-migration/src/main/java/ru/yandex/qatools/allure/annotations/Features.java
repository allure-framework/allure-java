package ru.yandex.qatools.allure.annotations;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * In order to group your tests by features simply annotate test suite
 * or test case with {@link ru.yandex.qatools.allure.annotations.Features}
 * annotation. This annotation can take either one string or a string
 * array because one test case can relate to several features:
 * <p/>
 * <pre>
 * &#064;Features({"Feature1", "Feature2"})
 * &#064;Test
 * public void myTest() {
 *     ...
 * }
 *
 * &#064;Features("Feature")
 * &#064;Test
 * public void myTest() {
 *     ...
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Features {

    String[] value();

}
