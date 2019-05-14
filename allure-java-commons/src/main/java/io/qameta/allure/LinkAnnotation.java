package io.qameta.allure;

import java.lang.annotation.*;

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
@Repeatable(LinkAnnotations.class)
public @interface LinkAnnotation {
    String value() default "";

    String type() default "link";
}
