package io.qameta.allure.junit4.samples;

import io.qameta.allure.Issue;
import io.qameta.allure.Link;
import io.qameta.allure.TmsLink;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
public class FailedTest {

    @Test
    @Link("link-1")
    @Link("link-2")
    @Issue("issue-1")
    @Issue("issue-2")
    @TmsLink("tms-1")
    @TmsLink("tms-2")
    public void failedTest() throws Exception {
        assertThat(true).isFalse();
    }
}
