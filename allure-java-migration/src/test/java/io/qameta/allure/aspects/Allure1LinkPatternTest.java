package io.qameta.allure.aspects;

import io.qameta.allure.model.Label;
import io.qameta.allure.util.ResultsUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import java.security.Key;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * eroshenkoam
 * 02.07.17
 */
public class Allure1LinkPatternTest {

    private static final String KEY = "ISSUE-1";

    @Before
    public void setSystemProperties() {
        System.setProperty("allure.link.issue.pattern", "https://issue/{}");
        System.setProperty("allure.link.tms.pattern", "https://tms/{}");
    }

    @Test
    public void issuePatternTest() {
        Issue issue = mock(Issue.class);
        when(issue.value()).thenReturn(KEY);

        Label label = Allure1Utils.createLabels(issue).get(0);
        assertThat(label.getValue())
                .isEqualTo(ResultsUtils.createIssueLink(KEY).getUrl());
    }

    @Test
    public void tmsPatternTest() {
        TestCaseId issue = mock(TestCaseId.class);
        when(issue.value()).thenReturn(KEY);

        Label label = Allure1Utils.createLabels(issue).get(0);
        assertThat(label.getValue())
                .isEqualTo(ResultsUtils.createTmsLink(KEY).getUrl());
    }

    @After
    public void cleanSystemProperties() {
        System.clearProperty("allure.link.issue.pattern");
        System.clearProperty("allure.link.tms.pattern");
    }

}
