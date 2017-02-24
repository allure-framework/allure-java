package io.qameta.allure.test;

import io.qameta.allure.ResultsUtils;
import io.qameta.allure.model.Link;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import static io.qameta.allure.ResultsUtils.getLinkTypePatternPropertyName;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
@RunWith(Parameterized.class)
public class LinksTests {

    @Parameterized.Parameter
    public String value;

    @Parameterized.Parameter(1)
    public String name;

    @Parameterized.Parameter(2)
    public String url;

    @Parameterized.Parameter(3)
    public String type;

    @Parameterized.Parameter(4)
    public String sysProp;

    @Parameterized.Parameter(5)
    public Link expected;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[]{"a", "b", "c", "d", "e", link("a", "c", "d")},
                new Object[]{"a", "b", "c", "d", null, link("a", "c", "d")},
                new Object[]{"a", "b", null, "d", "invalid-pattern", link("a", "invalid-pattern", "d")},
                new Object[]{"a", "b", null, "d", "pattern/{}/some", link("a", "pattern/a/some", "d")},
                new Object[]{null, null, null, "d", "pattern/{}/some", link(null, "pattern//some", "d")},
                new Object[]{null, null, null, null, "pattern/{}/some", link(null, null, null)},
                new Object[]{null, "b", null, "d", "pattern/{}/some/{}/and-more", link("b", "pattern/b/some/b/and-more", "d")},
                new Object[]{null, "b", null, "d", null, link("b", null, "d")}
        );
    }

    @Before
    public void setUp() throws Exception {
        if (Objects.nonNull(type) && Objects.nonNull(sysProp)) {
            System.setProperty(getLinkTypePatternPropertyName(type), sysProp);
        }
    }

    @Test
    public void shouldCreateLink() throws Exception {
        Link actual = ResultsUtils.createLink(value, name, url, type);
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", expected.getName())
                .hasFieldOrPropertyWithValue("url", expected.getUrl())
                .hasFieldOrPropertyWithValue("type", expected.getType());
    }

    @After
    public void tearDown() throws Exception {
        if (Objects.nonNull(type) && Objects.nonNull(sysProp)) {
            System.clearProperty(getLinkTypePatternPropertyName(type));
        }
    }

    private static Link link(String name, String url, String type) {
        return new Link().withName(name).withUrl(url).withType(type);
    }
}
