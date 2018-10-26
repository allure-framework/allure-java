package io.qameta.allure;

import io.qameta.allure.model.Link;
import io.qameta.allure.util.ResultsUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Objects;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.getLinkTypePatternPropertyName;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
class LinksTests {

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
                                 final Link expected) throws Exception {
        setSystemProperty(type, sysProp);
        try {
            Link actual = ResultsUtils.createLink(value, name, url, type);
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

    private static Link link(String name, String url, String type) {
        return new Link().setName(name).setUrl(url).setType(type);
    }
}
