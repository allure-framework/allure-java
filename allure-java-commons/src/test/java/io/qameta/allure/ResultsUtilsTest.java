/*
 *  Copyright 2016-2024 Qameta Software Inc
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
package io.qameta.allure;

import io.github.glytching.junit.extension.system.SystemProperty;
import io.github.glytching.junit.extension.system.SystemPropertyExtension;
import io.qameta.allure.util.ResultsUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.ISSUE_LINK_TYPE;
import static io.qameta.allure.util.ResultsUtils.TMS_LINK_TYPE;
import static io.qameta.allure.util.ResultsUtils.createIssueLink;
import static io.qameta.allure.util.ResultsUtils.createLink;
import static io.qameta.allure.util.ResultsUtils.createTmsLink;
import static io.qameta.allure.util.ResultsUtils.getLinkTypePatternPropertyName;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
@ExtendWith(SystemPropertyExtension.class)
class ResultsUtilsTest {

    @Test
    void shouldCreateLink() {
        io.qameta.allure.model.Link actual = createLink("a", "b", "c", "d");
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "a")
                .hasFieldOrPropertyWithValue("url", "c")
                .hasFieldOrPropertyWithValue("type", "d");
    }

    @Test
    void shouldCreateLinkFromAnnotation() {
        io.qameta.allure.model.Link actual = createLink(new Link() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Link.class;
            }

            @Override
            public String value() {
                return "a_from_annotation";
            }

            @Override
            public String name() {
                return "b_from_annotation";
            }

            @Override
            public String url() {
                return "c_from_annotation";
            }

            @Override
            public String type() {
                return "d_from_annotation";
            }
        });
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "a_from_annotation")
                .hasFieldOrPropertyWithValue("url", "c_from_annotation")
                .hasFieldOrPropertyWithValue("type", "d_from_annotation");
    }

    @SystemProperty(name = "allure.link.issue.pattern", value = "https://example.org/issue/{}")
    @Test
    void shouldCreateIssueLink() {
        io.qameta.allure.model.Link actual = createIssueLink("issue_link");
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "issue_link")
                .hasFieldOrPropertyWithValue("url", "https://example.org/issue/issue_link")
                .hasFieldOrPropertyWithValue("type", ISSUE_LINK_TYPE);
    }

    @SystemProperty(name = "allure.link.issue.pattern", value = "https://example.org/issue/{}")
    @Test
    void shouldCreateIssueLinkFromAnnotation() {
        io.qameta.allure.model.Link actual = createLink(new Issue() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Issue.class;
            }

            @Override
            public String value() {
                return "issue_link_from_annotation";
            }
        });
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "issue_link_from_annotation")
                .hasFieldOrPropertyWithValue("url", "https://example.org/issue/issue_link_from_annotation")
                .hasFieldOrPropertyWithValue("type", ISSUE_LINK_TYPE);
    }

    @SystemProperty(name = "allure.link.tms.pattern", value = "https://example.org/tms/{}")
    @Test
    void shouldCreateTmsLink() {
        io.qameta.allure.model.Link actual = createTmsLink("tms_link");
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "tms_link")
                .hasFieldOrPropertyWithValue("url", "https://example.org/tms/tms_link")
                .hasFieldOrPropertyWithValue("type", TMS_LINK_TYPE);
    }

    @SystemProperty(name = "allure.link.tms.pattern", value = "https://example.org/tms/{}")
    @Test
    void shouldCreateTmsLinkFromAnnotation() {
        io.qameta.allure.model.Link actual = createLink(new TmsLink() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return TmsLink.class;
            }

            @Override
            public String value() {
                return "tms_link_from_annotation";
            }
        });
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "tms_link_from_annotation")
                .hasFieldOrPropertyWithValue("url", "https://example.org/tms/tms_link_from_annotation")
                .hasFieldOrPropertyWithValue("type", TMS_LINK_TYPE);
    }

    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of("a", "b", "c", "d", "e", link("a", "c", "d")),
                Arguments.of("a", "b", "c", "d", null, link("a", "c", "d")),
                Arguments.of("a", "b", null, "d", "invalid-pattern", link("a", "invalid-pattern", "d")),
                Arguments.of("a", "b", null, "d", "pattern/{}/some", link("a", "pattern/a/some", "d")),
                Arguments.of(null, null, null, "d", "pattern/{}/some", link(null, "pattern//some", "d")),
                Arguments.of(null, null, null, null, "pattern/{}/some", link(null, null, null)),
                Arguments.of(null, "b", null, "d", "pattern/{}/some/{}/and-more", link("b", "pattern/b/some/b/and-more", "d")),
                Arguments.of(null, "b", null, "d", null, link("b", null, "d"))
        );
    }

    public void setSystemProperty(final String type, final String sysProp) {
        if (Objects.nonNull(type) && Objects.nonNull(sysProp)) {
            System.setProperty(getLinkTypePatternPropertyName(type), sysProp);
        }
    }

    @ParameterizedTest
    @MethodSource(value = "data")
    public void shouldCreateLink(final String value,
                                 final String name,
                                 final String url,
                                 final String type,
                                 final String sysProp,
                                 final io.qameta.allure.model.Link expected) {
        setSystemProperty(type, sysProp);
        try {
            io.qameta.allure.model.Link actual = ResultsUtils.createLink(value, name, url, type);
            assertThat(actual)
                    .isNotNull()
                    .hasFieldOrPropertyWithValue("name", expected.getName())
                    .hasFieldOrPropertyWithValue("url", expected.getUrl())
                    .hasFieldOrPropertyWithValue("type", expected.getType());
        } finally {
            clearSystemProperty(type, sysProp);
        }
    }

    public void clearSystemProperty(final String type, final String sysProp) {
        if (Objects.nonNull(type) && Objects.nonNull(sysProp)) {
            System.clearProperty(getLinkTypePatternPropertyName(type));
        }
    }

    private static io.qameta.allure.model.Link link(String name, String url, String type) {
        return new io.qameta.allure.model.Link().setName(name).setUrl(url).setType(type);
    }
}
