package io.qameta.allure.util;

import io.qameta.allure.Issue;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
class AspectUtilsTest {

    @Issue("191")
    @Test
    void shouldProcessToStringNpe() {
        final MyNpeClass myNpeClass = new MyNpeClass();
        final String string = AspectUtils.objectToString(myNpeClass);
        assertThat(string)
                .isEqualTo("<NPE>");
    }

    public class MyNpeClass {

        Integer value = null;

        @Override
        public String toString() {
            return value.toString();
        }
    }

}