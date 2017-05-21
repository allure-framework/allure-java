package io.qameta.allure;

import io.qameta.allure.aspects.StepsAspects;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.testdata.AllureResultsWriterStub;
import io.qameta.allure.testdata.DummyCard;
import io.qameta.allure.testdata.DummyUser;
import io.qameta.allure.testdata.DummyEmail;
import org.junit.Test;

import java.util.UUID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sskorol (Sergey Korol)
 */
public class StepsTests {

    @Test
    public void shouldTransformPlaceholdersToPropertyValues() {
        final AllureResultsWriterStub results = new AllureResultsWriterStub();
        final AllureLifecycle lifecycle = new AllureLifecycle(results);
        StepsAspects.setLifecycle(lifecycle);

        final String uuid = UUID.randomUUID().toString();
        final TestResult result = new TestResult().withUuid(uuid);

        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);

        final DummyEmail[] emails = new DummyEmail[]{
                new DummyEmail("test1@email.com", asList("txt", "png")),
                new DummyEmail("test2@email.com", asList("jpg", "mp4")),
                null
        };
        final DummyCard card = new DummyCard("1111222233334444");
        final DummyUser nullUser = null;

        loginWith(new DummyUser(emails, "12345678", card), nullUser, true);

        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);

        assertThat(results.getTestResults())
                .flatExtracting(TestResult::getSteps)
                .extracting(StepResult::getName)
                .containsExactly("\"[test1@email.com, test2@email.com, null]\"," +
                        " \"[{address='test1@email.com', attachments='[txt, png]'}," +
                        " {address='test2@email.com', attachments='[jpg, mp4]'}," +
                        " null]\"," +
                        " \"[[txt, png], [jpg, mp4], null]\"," +
                        " \"12345678\", \"{}\","
                        + " \"1111222233334444\", \"{missing}\", null, true");
    }

    @Step("\"{user1.emails.address}\", \"{user1.emails}\", \"{user1.emails.attachments}\", \"{user1.password}\", \"{}\"," +
            " \"{user1.card.number}\", \"{missing}\", {user2}, {staySignedIn}")
    private void loginWith(final DummyUser user1, final DummyUser user2, final boolean staySignedIn) {
    }
}
