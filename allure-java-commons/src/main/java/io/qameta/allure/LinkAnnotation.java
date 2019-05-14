package io.qameta.allure;

import java.lang.annotation.*;

import static io.qameta.allure.util.ResultsUtils.LINK_TYPE;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface LinkAnnotation {
    String value() default "";

    String type() default LINK_TYPE;
}
