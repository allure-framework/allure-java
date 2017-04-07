package io.qameta.allure;

import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static io.qameta.allure.testdata.TestData.randomString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureLifecycleTest {

    private AllureResultsWriter writer;
    private AllureLifecycle lifecycle;

    @Before
    public void setUp() throws Exception {
        writer = Mockito.mock(AllureResultsWriter.class);
        lifecycle = new AllureLifecycle(writer);
    }

    @Test
    public void shouldCreateTest() throws Exception {
        final String uuid = randomString();
        final String name = randomString();
        final TestResult result = new TestResult().withUuid(uuid).withName(name);
        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);
        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer, times(1)).write(captor.capture());

        assertThat(captor.getValue())
                .isNotNull()
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("name", name);
    }

    @Test
    public void shouldCreateTestContainer() throws Exception {
        final String uuid = randomString();
        final String name = randomString();
        final TestResultContainer container = new TestResultContainer()
                .withUuid(uuid)
                .withName(name);
        lifecycle.startTestContainer(container);
        lifecycle.stopTestContainer(uuid);
        lifecycle.writeTestContainer(uuid);

        final ArgumentCaptor<TestResultContainer> captor = forClass(TestResultContainer.class);
        verify(writer, times(1)).write(captor.capture());

        assertThat(captor.getValue())
                .isNotNull()
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("name", name);

    }

    @Test
    public void shouldCreateChildTestContainer() throws Exception {
        final String parentUuid = randomString();
        final String parentName = randomString();
        final TestResultContainer parent = new TestResultContainer()
                .withUuid(parentUuid)
                .withName(parentName);
        lifecycle.startTestContainer(parent);

        final String childUuid = randomString();
        final String childName = randomString();
        final TestResultContainer container = new TestResultContainer()
                .withUuid(childUuid)
                .withName(childName);
        lifecycle.startTestContainer(parentUuid, container);
        lifecycle.stopTestContainer(childUuid);
        lifecycle.writeTestContainer(childUuid);

        lifecycle.stopTestContainer(parentUuid);
        lifecycle.writeTestContainer(parentUuid);

        final ArgumentCaptor<TestResultContainer> captor = forClass(TestResultContainer.class);
        verify(writer, times(2)).write(captor.capture());

        final List<TestResultContainer> values = captor.getAllValues();
        assertThat(values)
                .isNotNull()
                .hasSize(2);

        assertThat(values.get(0))
                .isNotNull()
                .hasFieldOrPropertyWithValue("uuid", childUuid)
                .hasFieldOrPropertyWithValue("name", childName);

        final TestResultContainer actual = values.get(1);
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("uuid", parentUuid)
                .hasFieldOrPropertyWithValue("name", parentName)
                .extracting(TestResultContainer::getChildren)
                .containsExactly(Collections.singletonList(childUuid));
    }

    @Test
    public void shouldAddStepsToTests() throws Exception {
        final String uuid = randomString();
        final String name = randomString();
        final TestResult result = new TestResult().withUuid(uuid).withName(name);
        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);

        final String firstStepName = randomStep(uuid);
        final String secondStepName = randomStep(uuid);

        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer, times(1)).write(captor.capture());

        final TestResult actual = captor.getValue();
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("name", name);

        assertThat(actual.getSteps())
                .flatExtracting(StepResult::getName)
                .containsExactly(firstStepName, secondStepName);
    }

    @Test
    public void shouldUpdateTest() throws Exception {
        final String uuid = randomString();
        final String name = randomString();

        final TestResult result = new TestResult().withUuid(uuid).withName(name);
        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);

        final String stepUuid = randomString();
        final String stepName = randomString();

        final StepResult step = new StepResult().withName(stepName);
        lifecycle.startStep(uuid, stepUuid, step);

        final String description = randomString();
        final String fullName = randomString();

        lifecycle.updateTestCase(uuid, testResult -> testResult.withDescription(description));
        lifecycle.updateTestCase(testResult -> testResult.withFullName(fullName));

        lifecycle.stopStep(stepUuid);

        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);

        final ArgumentCaptor<TestResult> captor = forClass(TestResult.class);
        verify(writer, times(1)).write(captor.capture());

        final TestResult actual = captor.getValue();
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("description", description)
                .hasFieldOrPropertyWithValue("name", name)
                .hasFieldOrPropertyWithValue("fullName", fullName);

        assertThat(actual.getSteps())
                .flatExtracting(StepResult::getName)
                .containsExactly(stepName);
    }

    @Test
    public void shouldUpdateContainer() throws Exception {
        final String uuid = randomString();
        final String name = randomString();
        final String newName = randomString();

        final TestResultContainer container = new TestResultContainer()
                .withUuid(uuid)
                .withName(name);
        lifecycle.startTestContainer(container);

        lifecycle.updateTestContainer(uuid, c -> c.withName(newName));
        lifecycle.stopTestContainer(uuid);
        lifecycle.writeTestContainer(uuid);

        ArgumentCaptor<TestResultContainer> captor = forClass(TestResultContainer.class);
        verify(writer, times(1)).write(captor.capture());

        assertThat(captor.getValue())
                .isNotNull()
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("name", newName);
    }

    @Test
    public void shouldCreateTestFixture() throws Exception {
        final String uuid = randomString();
        final String name = randomString();

        TestResultContainer container = new TestResultContainer()
                .withUuid(uuid)
                .withName(name);
        lifecycle.startTestContainer(container);

        final String firstUuid = randomString();
        final String firstName = randomString();
        final FixtureResult first = new FixtureResult().withName(firstName);

        lifecycle.startPrepareFixture(uuid, firstUuid, first);

        final String firstStepName = randomStep(firstUuid);
        final String secondStepName = randomStep(firstUuid);

        lifecycle.stopFixture(firstUuid);

        lifecycle.stopTestContainer(uuid);
        lifecycle.writeTestContainer(uuid);

        final ArgumentCaptor<TestResultContainer> captor = forClass(TestResultContainer.class);
        verify(writer, times(1)).write(captor.capture());

        final TestResultContainer actual = captor.getValue();
        assertThat(actual)
                .isNotNull()
                .hasFieldOrPropertyWithValue("uuid", uuid)
                .hasFieldOrPropertyWithValue("name", name);

        assertThat(actual.getBefores())
                .hasSize(1);

        final FixtureResult fixtureResult = actual.getBefores().get(0);
        assertThat(fixtureResult)
                .isNotNull()
                .hasFieldOrPropertyWithValue("name", firstName);

        assertThat(fixtureResult.getSteps())
                .hasSize(2)
                .flatExtracting(StepResult::getName)
                .containsExactly(firstStepName, secondStepName);
    }

    private String randomStep(String parentUuid) {
        final String uuid = randomString();
        final String name = randomString();
        final StepResult step = new StepResult().withName(name);
        lifecycle.startStep(parentUuid, uuid, step);
        lifecycle.stopStep(uuid);
        return name;
    }
}