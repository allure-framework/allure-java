package io.qameta.allure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static io.qameta.allure.util.ResultsUtils.LEAD_LABEL_NAME;

/**
 * This annotation used to specify project leads for test case.
 *
 * @see Owner
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@LabelAnnotation(name = LEAD_LABEL_NAME)
public @interface Lead {

    String value();

}
