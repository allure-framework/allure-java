/*
 *  Copyright 2019 Qameta Software OÜ
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.util;

import io.github.glytching.junit.extension.system.SystemProperty;
import io.qameta.allure.Epic;
import io.qameta.allure.Epics;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Issues;
import io.qameta.allure.LabelAnnotation;
import io.qameta.allure.Link;
import io.qameta.allure.LinkAnnotation;
import io.qameta.allure.Links;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.TmsLink;
import io.qameta.allure.TmsLinks;
import io.qameta.allure.model.Label;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import static io.qameta.allure.util.AnnotationUtils.getLabels;
import static io.qameta.allure.util.AnnotationUtils.getLinks;
import static io.qameta.allure.util.ResultsUtils.EPIC_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.FEATURE_LABEL_NAME;
import static io.qameta.allure.util.ResultsUtils.STORY_LABEL_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

/**
 * @author charlie (Dmitry Baev).
 */
class AnnotationUtilsTest {

    @Epic("e1")
    @Feature("f1")
    @Story("s1")
    class WithBddAnnotations {
    }

    @Test
    void shouldExtractDefaultLabels() {
        final Set<Label> labels = getLabels(WithBddAnnotations.class);
        assertThat(labels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple(EPIC_LABEL_NAME, "e1"),
                        tuple(FEATURE_LABEL_NAME, "f1"),
                        tuple(STORY_LABEL_NAME, "s1")
                );
    }

    @Epic("e1")
    @Epic("e2")
    class RepeatableAnnotations {
    }

    @Test
    void shouldExtractRepeatableLabels() {
        final Set<Label> labels = getLabels(RepeatableAnnotations.class);
        assertThat(labels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple(EPIC_LABEL_NAME, "e1"),
                        tuple(EPIC_LABEL_NAME, "e2")
                );
    }

    @Epics({
            @Epic("e1"),
            @Epic("e2")
    })
    class DirectRepeatableAnnotations {
    }

    @Test
    void shouldExtractDirectRepeatableLabels() {
        final Set<Label> labels = getLabels(DirectRepeatableAnnotations.class);
        assertThat(labels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple(EPIC_LABEL_NAME, "e1"),
                        tuple(EPIC_LABEL_NAME, "e2")
                );
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = "custom")
    @interface Custom {

        String value();

    }

    @Custom("Some")
    class CustomAnnotation {
    }

    @Test
    void shouldExtractCustomAnnotations() {
        final Set<Label> labels = getLabels(CustomAnnotation.class);
        assertThat(labels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("custom", "Some")
                );
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = "custom")
    @interface CustomMultiValue {

        String[] value();

    }

    @CustomMultiValue({"First", "Second"})
    class CustomMultiValueAnnotation {
    }

    @Test
    void shouldSupportMultiValueAnnotations() {
        final Set<Label> labels = getLabels(CustomMultiValueAnnotation.class);
        assertThat(labels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("custom", "First"),
                        tuple("custom", "Second")
                );
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = "custom", value = "fixed")
    @interface CustomFixed {
    }

    @CustomFixed
    class CustomFixedAnnotation {
    }

    @Test
    void shouldSupportCustomFixedAnnotations() {
        final Set<Label> labels = getLabels(CustomFixedAnnotation.class);
        assertThat(labels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("custom", "fixed")
                );
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = "a", value = "a1")
    @LabelAnnotation(name = "a", value = "a2")
    @LabelAnnotation(name = "b", value = "b1")
    @LabelAnnotation(name = "b", value = "b2")
    @interface CustomMultiLabel {
    }

    @CustomMultiLabel
    class CustomMultiLabelAnnotation {
    }

    @Test
    void shouldSupportCustomMultiLabelAnnotations() {
        final Set<Label> labels = getLabels(CustomMultiLabelAnnotation.class);
        assertThat(labels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("a", "a1"),
                        tuple("a", "a2"),
                        tuple("b", "b1"),
                        tuple("b", "b2")
                );
    }

    @Link(name = "LINK-1")
    @Links({
            @Link(name = "LINK-2", url = "https://example.org/link/2"),
            @Link(url = "https://example.org/some-custom-link")
    })
    @TmsLink("TMS-1")
    @TmsLinks({
            @TmsLink("TMS-2"),
            @TmsLink("TMS-3")
    })
    @Issue("ISSUE-1")
    @Issues({
            @Issue("ISSUE-2"),
            @Issue("ISSUE-3")
    })
    class WithLinks {
    }

    @ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
    @SystemProperty(name = "allure.link.issue.pattern", value = "https://example.org/issue/{}")
    @SystemProperty(name = "allure.link.tms.pattern", value = "https://example.org/tms/{}")
    @SystemProperty(name = "allure.link.custom.pattern", value = "https://example.org/custom/{}")
    @Test
    void shouldExtractLinks() {
        assertThat(getLinks(WithLinks.class))
                .extracting(
                        io.qameta.allure.model.Link::getName,
                        io.qameta.allure.model.Link::getType,
                        io.qameta.allure.model.Link::getUrl)
                .contains(
                        tuple("LINK-1", "custom", "https://example.org/custom/LINK-1"),
                        tuple("LINK-2", "custom", "https://example.org/link/2"),
                        tuple("", "custom", "https://example.org/some-custom-link"),
                        tuple("ISSUE-1", "issue", "https://example.org/issue/ISSUE-1"),
                        tuple("ISSUE-2", "issue", "https://example.org/issue/ISSUE-2"),
                        tuple("ISSUE-3", "issue", "https://example.org/issue/ISSUE-3"),
                        tuple("TMS-1", "tms", "https://example.org/tms/TMS-1"),
                        tuple("TMS-2", "tms", "https://example.org/tms/TMS-2"),
                        tuple("TMS-3", "tms", "https://example.org/tms/TMS-3")
                );
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LinkAnnotation
    @interface CustomLink {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LinkAnnotation(type = "issue")
    @interface CustomIssue {
        String value();
    }

    @CustomLink("LINK-2")
    @Link("LINK-1")
    @CustomIssue("ISSUE-1")
    class WithCustomLink {
    }

    @ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
    @SystemProperty(name = "allure.link.custom.pattern", value = "https://example.org/custom/{}")
    @SystemProperty(name = "allure.link.issue.pattern", value = "https://example.org/issue/{}")
    @Test
    void shouldExtractCustomLinks() {
        assertThat(getLinks(WithCustomLink.class))
                .extracting(io.qameta.allure.model.Link::getUrl)
                .containsOnly(
                        "https://example.org/custom/LINK-2",
                        "https://example.org/custom/LINK-1",
                        "https://example.org/issue/ISSUE-1"
                );
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Feature("First")
    @Issue("1")
    @interface FirstFeature {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Feature("Second")
    @Issue("2")
    @interface SecondFeature {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @FirstFeature
    @SecondFeature
    @Story("Other")
    @interface OtherStory {
    }

    @OtherStory
    class WithMultiFeature {
    }

    @Test
    void shouldSupportMultiFeature() {
        final Set<Label> labels = getLabels(WithMultiFeature.class);
        assertThat(labels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("feature", "First"),
                        tuple("feature", "Second"),
                        tuple("story", "Other")
                );
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Recursive("second")
    @LabelAnnotation(name = "recursive")
    @interface Recursive {
        String value();
    }

    @Recursive("first")
    class WithRecurse {
    }

    @Test
    void shouldExtractLabelsFromRecursiveAnnotations() {
        final Set<Label> labels = getLabels(WithRecurse.class);
        assertThat(labels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("recursive", "first"),
                        tuple("recursive", "second")
                );
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface Features {

        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.TYPE})
        @Feature("A")
        @Owner("tester1")
        @Link(url = "https://example.org/features/A")
        @interface FeatureA {

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.METHOD, ElementType.TYPE})
            @FeatureA
            @Story("s1")
            @interface Story1 {
            }
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target({ElementType.TYPE})
        @Feature("B")
        @Owner("tester2")
        @Link(url = "https://example.org/features/B")
        @interface FeatureB {

            @Retention(RetentionPolicy.RUNTIME)
            @Target({ElementType.METHOD, ElementType.TYPE})
            @FeatureB
            @Story("s2")
            @interface Story2 {
            }
        }
    }

    @Features.FeatureA.Story1
    class Test1 {
    }

    @Features.FeatureB.Story2
    class Test2 {
    }

    @Features.FeatureA.Story1
    @Features.FeatureB.Story2
    class Test3 {
    }

    @Test
    void complexCase() {
        final Set<Label> labels1 = getLabels(Test1.class);
        assertThat(labels1)
                .extracting(Label::getName, Label::getValue)
                .containsExactlyInAnyOrder(
                        tuple("feature", "A"),
                        tuple("story", "s1"),
                        tuple("owner", "tester1")
                );

        final Set<Label> labels2 = getLabels(Test2.class);
        assertThat(labels2)
                .extracting(Label::getName, Label::getValue)
                .containsExactlyInAnyOrder(
                        tuple("feature", "B"),
                        tuple("story", "s2"),
                        tuple("owner", "tester2")
                );

        final Set<Label> labels3 = getLabels(Test3.class);
        assertThat(labels3)
                .extracting(Label::getName, Label::getValue)
                .containsExactlyInAnyOrder(
                        tuple("feature", "A"),
                        tuple("story", "s1"),
                        tuple("owner", "tester1"),
                        tuple("feature", "B"),
                        tuple("story", "s2"),
                        tuple("owner", "tester2")
                );
    }
}
