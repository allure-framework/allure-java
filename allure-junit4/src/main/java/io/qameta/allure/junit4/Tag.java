package io.qameta.allure.junit4;

import java.lang.annotation.*;

/**
 * @author jkttt on 05.07.17.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(Tags.class)
public @interface Tag {

    String value();

}
