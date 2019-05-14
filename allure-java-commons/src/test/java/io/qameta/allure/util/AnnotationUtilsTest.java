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
import io.qameta.allure.*;
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
import static io.qameta.allure.util.ResultsUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE;
import static org.junit.jupiter.api.parallel.Resources.SYSTEM_PROPERTIES;

/**
 * @author charlie (Dmitry Baev).
 */
class AnnotationUtilsTest {

    @Test
    void shouldExtractEpic() {
        final Set<Label> labels = getLabels(WithBddAnnotations.class);
        assertThat(labels)
                .extracting(Label::getName, Label::getValue)
                .contains(tuple(EPIC_LABEL_NAME, "e1"));
    }

    @Test
    void shouldExtractLabelsFromGivenAnnotations() {
        final Set<Label> labels = getLabels(WithBddAnnotations.class.getDeclaredAnnotations());
        assertThat(labels)
                .extracting(Label::getName, Label::getValue)
                .contains(tuple(EPIC_LABEL_NAME, "e1"));
    }

    @Test
    void shouldExtractFeature() {
        final Set<Label> labels = getLabels(WithBddAnnotations.class);
        assertThat(labels)
                .extracting(Label::getName, Label::getValue)
                .contains(tuple(FEATURE_LABEL_NAME, "f1"));
    }

    @Test
    void shouldExtractStory() {
        final Set<Label> labels = getLabels(WithBddAnnotations.class);
        assertThat(labels)
                .extracting(Label::getName, Label::getValue)
                .contains(tuple(STORY_LABEL_NAME, "s1"));
    }

    @Test
    void shouldExtractRepeatableEpic() {
        final Set<Label> labels = getLabels(RepeatableAnnotations.class);
        assertThat(labels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple(EPIC_LABEL_NAME, "e1"),
                        tuple(EPIC_LABEL_NAME, "e2")
                );
    }

    @Test
    void shouldExtractDirectRepeatableEpic() {
        final Set<Label> labels = getLabels(DirectRepeatableAnnotations.class);
        assertThat(labels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple(EPIC_LABEL_NAME, "e1"),
                        tuple(EPIC_LABEL_NAME, "e2")
                );
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

    @Test
    void shouldSupportCustomFixedAnnotations() {
        final Set<Label> labels = getLabels(CustomFixedAnnotation.class);
        assertThat(labels)
                .extracting(Label::getName, Label::getValue)
                .contains(
                        tuple("custom", "fixed")
                );
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

    @ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
    @SystemProperty(name = "allure.link.issue.pattern", value = "https://example.org/issue/{}")
    @SystemProperty(name = "allure.link.tms.pattern", value = "https://example.org/tms/{}")
    @SystemProperty(name = "allure.link.custom.pattern", value = "https://example.org/custom/{}")
    @Test
    void shouldExtractLinks() {
        assertThat(getLinks(WithLinks.class))
                .extracting(io.qameta.allure.model.Link::getName, io.qameta.allure.model.Link::getType, io.qameta.allure.model.Link::getUrl)
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

    @ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
    @SystemProperty(name = "allure.link.issue.pattern", value = "https://example.org/issue/{}")
    @SystemProperty(name = "allure.link.tms.pattern", value = "https://example.org/tms/{}")
    @SystemProperty(name = "allure.link.custom.pattern", value = "https://example.org/custom/{}")
    @Test
    void shouldExtractLinksFromAnnotationList() {
        assertThat(getLinks(WithLinks.class.getDeclaredAnnotations()))
                .extracting(io.qameta.allure.model.Link::getName, io.qameta.allure.model.Link::getType, io.qameta.allure.model.Link::getUrl)
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

    @ResourceLock(value = SYSTEM_PROPERTIES, mode = READ_WRITE)
    @SystemProperty(name = "allure.link.custom.pattern", value = "https://example.org/custom/{}")
    @SystemProperty(name = "allure.link.tms.pattern", value = "https://tms.com/custom/{}")
    @Test
    void shouldExtractCustomLinks() {
        assertThat(getLinks(WithCustomLink.class.getDeclaredAnnotations()))
                .extracting(io.qameta.allure.model.Link::getUrl)
                .containsOnly("https://example.org/custom/LINK-2",
                        "https://example.org/custom/LINK-1",
                        "https://tms.com/custom/ISSUE-1");
    }

    @Epic("e1")
    @Feature("f1")
    @Story("s1")
    class WithBddAnnotations {
    }

    @Epic("e1")
    @Epic("e2")
    class RepeatableAnnotations {
    }

    @Epics({
            @Epic("e1"),
            @Epic("e2")
    })
    class DirectRepeatableAnnotations {
    }

    @Custom("Some")
    class CustomAnnotation {
    }

    @CustomMultiValue({"First", "Second"})
    class CustomMultiValueAnnotation {
    }

    @CustomFixed
    class CustomFixedAnnotation {
    }

    @CustomMultiLabel
    class CustomMultiLabelAnnotation {
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

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = "custom")
    public @interface Custom {

        String value();

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = "custom")
    public @interface CustomMultiValue {

        String[] value();

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = "custom", value = "fixed")
    public @interface CustomFixed {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LabelAnnotation(name = "a", value = "a1")
    @LabelAnnotation(name = "a", value = "a2")
    @LabelAnnotation(name = "b", value = "b1")
    @LabelAnnotation(name = "b", value = "b2")
    public @interface CustomMultiLabel {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LinkAnnotation
    public @interface CustomLink {
        String value() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @LinkAnnotation(type = "tms")
    public @interface CustomIssue {
        String value();
    }

    @CustomLink("LINK-2")
    @Link("LINK-1")
    @CustomIssue("ISSUE-1")
    class WithCustomLink { }
}
