package io.qameta.allure.junitplatform.features;

import io.qameta.allure.LabelAnnotation;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static io.qameta.allure.util.ResultsUtils.FEATURE_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.STORY_LABEL_NAME;

/**
 * @author jkttt on 05.07.17.
 */
public class MarkerAnnotationSupport {

    @Custom
    @Test
    void single() {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Basic framework support")
    @LabelAnnotation(name = STORY_LABEL_NAME, value = "Core features")
    public @interface Custom {
    }
}
