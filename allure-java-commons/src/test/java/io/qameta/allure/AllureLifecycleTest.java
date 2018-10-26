package io.qameta.allure;

import io.qameta.allure.model.FixtureResult;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static io.github.benas.randombeans.api.EnhancedRandom.random;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author charlie (Dmitry Baev).
 */
class AllureLifecycleTest {

    private AllureResultsWriter writer;
    private AllureLifecycle lifecycle;

    @BeforeEach
    public void setUp() {
        writer = Mockito.mock(AllureResultsWriter.class);
        lifecycle = new AllureLifecycle(writer);
    }

    @Test
    void shouldCreateTest() {
        final String uuid = random(String.class);
        final String name = random(String.class);
        final TestResult result = new TestResult().setUuid(uuid).setName(name);
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
    void shouldCreateTestContainer() {
        final String uuid = random(String.class);
        final String name = random(String.class);
        final TestResultContainer container = new TestResultContainer()
                .setUuid(uuid)
                .setName(name);
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
    void shouldCreateChildTestContainer() {
        final String parentUuid = random(String.class);
        final String parentName = random(String.class);
        final TestResultContainer parent = new TestResultContainer()
                .setUuid(parentUuid)
                .setName(parentName);
        lifecycle.startTestContainer(parent);

        final String childUuid = random(String.class);
        final String childName = random(String.class);
        final TestResultContainer container = new TestResultContainer()
                .setUuid(childUuid)
                .setName(childName);
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
    void shouldAddStepsToTests() {
        final String uuid = random(String.class);
        final String name = random(String.class);
        final TestResult result = new TestResult().setUuid(uuid).setName(name);
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
    void shouldUpdateTest() {
        final String uuid = random(String.class);
        final String name = random(String.class);

        final TestResult result = new TestResult().setUuid(uuid).setName(name);
        lifecycle.scheduleTestCase(result);
        lifecycle.startTestCase(uuid);

        final String stepUuid = random(String.class);
        final String stepName = random(String.class);

        final StepResult step = new StepResult().setName(stepName);
        lifecycle.startStep(uuid, stepUuid, step);

        final String description = random(String.class);
        final String fullName = random(String.class);

        lifecycle.updateTestCase(uuid, testResult -> testResult.setDescription(description));
        lifecycle.updateTestCase(testResult -> testResult.setFullName(fullName));

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
    void shouldUpdateContainer() {
        final String uuid = random(String.class);
        final String name = random(String.class);
        final String newName = random(String.class);

        final TestResultContainer container = new TestResultContainer()
                .setUuid(uuid)
                .setName(name);
        lifecycle.startTestContainer(container);

        lifecycle.updateTestContainer(uuid, c -> c.setName(newName));
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
    void shouldCreateTestFixture() {
        final String uuid = random(String.class);
        final String name = random(String.class);

        TestResultContainer container = new TestResultContainer()
                .setUuid(uuid)
                .setName(name);
        lifecycle.startTestContainer(container);

        final String firstUuid = random(String.class);
        final String firstName = random(String.class);
        final FixtureResult first = new FixtureResult().setName(firstName);

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
        final String uuid = random(String.class);
        final String name = random(String.class);
        final StepResult step = new StepResult().setName(name);
        lifecycle.startStep(parentUuid, uuid, step);
        lifecycle.stopStep(uuid);
        return name;
    }
}