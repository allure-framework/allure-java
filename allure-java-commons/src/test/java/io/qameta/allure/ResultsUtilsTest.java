package io.qameta.allure;

import io.qameta.allure.model.ExecutableItem;
import io.qameta.allure.util.ResultsUtils;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static io.qameta.allure.util.ResultsUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
public class ResultsUtilsTest {

    @Test
    public void shouldCreateLink() throws Exception {
        io.qameta.allure.model.Link actual = createLink("a", "b", "c", "d");
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "a")
                .hasFieldOrPropertyWithValue("url", "c")
                .hasFieldOrPropertyWithValue("type", "d");
    }

    @Test
    public void shouldCreateLinkFromAnnotation() throws Exception {
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

    @Test
    public void shouldCreateIssueLink() throws Exception {
        io.qameta.allure.model.Link actual = createIssueLink("issue_link");
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "issue_link")
                .hasFieldOrPropertyWithValue("url", null)
                .hasFieldOrPropertyWithValue("type", ISSUE_LINK_TYPE);
    }

    @Test
    public void shouldCreateIssueLinkFromAnnotation() throws Exception {
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
                .hasFieldOrPropertyWithValue("url", null)
                .hasFieldOrPropertyWithValue("type", ISSUE_LINK_TYPE);
    }

    @Test
    public void shouldCreateTmsLink() throws Exception {
        io.qameta.allure.model.Link actual = createTmsLink("tms_link");
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", "tms_link")
                .hasFieldOrPropertyWithValue("url", null)
                .hasFieldOrPropertyWithValue("type", TMS_LINK_TYPE);
    }

    @Test
    public void shouldCreateTmsLinkFromAnnotation() throws Exception {
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
                .hasFieldOrPropertyWithValue("url", null)
                .hasFieldOrPropertyWithValue("type", TMS_LINK_TYPE);
    }

    /**
     * Javadoc description is needed for proper checking of {@link ResultsUtils#processDescription}
     */
    @Description(useJavaDoc = true)
    @Test
    public void shouldNotThrowExceptionIfAllureDescriptionDoesNotExist() throws NoSuchMethodException {
        ExecutableItem mockedExecutableItem = new ExecutableItem() {};
        Method thisTestMethod = getClass().getMethod("shouldNotThrowExceptionIfAllureDescriptionDoesNotExist", (Class<?>[]) null);

        Exception actualException = null;
        try {
            processDescription(getClass().getClassLoader(), thisTestMethod, mockedExecutableItem);
        } catch (Exception e) {
            actualException = e;
        }
        assertThat(actualException)
                .isNull();
    }

}