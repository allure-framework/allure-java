package io.qameta.allure.test;

import io.qameta.allure.LabelAnnotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static io.qameta.allure.util.ResultsUtils.FEATURE_LABEL_NAME;

/**
 * @author charlie (Dmitry Baev).
 */
@SuppressWarnings({"JavadocType", "PMD.MissingStaticMethodInNonInstantiatableClass"})
public final class AllureFeatures {

    private AllureFeatures() {
        throw new IllegalStateException("Do not instance");
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Basic framework support")
    public @interface Base {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Parallel test execution support")
    public @interface Parallel {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Full name")
    public @interface FullName {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Display name")
    public @interface DisplayName {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Descriptions")
    public @interface Descriptions {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Timings")
    public @interface Timings {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Steps")
    public @interface Steps {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Attachments")
    public @interface Attachments {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Parameters")
    public @interface Parameters {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Fixtures")
    public @interface Fixtures {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Links")
    public @interface Links {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Marker annotations")
    public @interface MarkerAnnotations {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Failed tests")
    public @interface FailedTests {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Broken tests")
    public @interface BrokenTests {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Passed tests")
    public @interface PassedTests {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Skipped tests")
    public @interface SkippedTests {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Ignored tests")
    public @interface IgnoredTests {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Not implemented tests")
    public @interface NotImplementedTests {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "History")
    public @interface History {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Retries")
    public @interface Retries {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Stages")
    public @interface Stages {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Trees")
    public @interface Trees {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Timeline")
    public @interface Timeline {
    }

    @Documented
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = FEATURE_LABEL_NAME, value = "Timeline")
    public @interface Severity {
    }
}
