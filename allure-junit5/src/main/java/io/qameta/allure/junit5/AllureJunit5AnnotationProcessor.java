package io.qameta.allure.junit5;

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Allure Junit5 annotation processor.
 *
 * @deprecated use Allure JUnit platform integration instead
 */
@Deprecated
public class AllureJunit5AnnotationProcessor implements BeforeTestExecutionCallback {

    @Override
    public void beforeTestExecution(final ExtensionContext context) {
        //do nothing
    }
}
