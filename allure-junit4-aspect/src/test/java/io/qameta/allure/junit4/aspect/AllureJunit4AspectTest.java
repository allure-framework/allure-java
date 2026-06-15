/*
 *  Copyright 2016-2026 Qameta Software Inc
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
package io.qameta.allure.junit4.aspect;

import io.qameta.allure.Allure;
import io.qameta.allure.junit4.AllureJunit4;
import io.qameta.allure.junit4.AllureJunit4Filter;
import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AllureJunit4AspectTest {

    @Test
    void shouldApplyAllureFilterToFilterableRunner() {
        final TrackingRunner runner = new TrackingRunner(false);

        Allure.step(
                "Apply the Allure JUnit 4 filter aspect to a filterable runner",
                () -> new AllureJunit4FilterAspect().filterBeforeRun(runner)
        );

        Allure.step("Verify the runner received the Allure filter", () -> {
            assertThat(runner.appliedFilter)
                    .isInstanceOf(AllureJunit4Filter.class);
        });
    }

    @Test
    void shouldIgnoreNoTestsRemainExceptionDuringFiltering() {
        final TrackingRunner runner = new TrackingRunner(true);

        Allure.step("Apply the filter aspect when the runner reports no remaining tests", step -> {
            step.parameter("throws NoTestsRemainException", true);
            assertThatCode(() -> new AllureJunit4FilterAspect().filterBeforeRun(runner))
                    .doesNotThrowAnyException();
        });

        Allure.step("Verify the Allure filter was applied before the exception was ignored", () -> {
            assertThat(runner.appliedFilter)
                    .isInstanceOf(AllureJunit4Filter.class);
        });
    }

    @Test
    void shouldAddListenerToPlainRunNotifier() throws Exception {
        final RunNotifier notifier = new RunNotifier();
        final JoinPoint point = mock(JoinPoint.class);
        when(point.getThis()).thenReturn(notifier);

        Allure.step(
                "Invoke the listener aspect against a plain RunNotifier", () -> new AllureJunit4ListenerAspect().addListener(point)
        );

        Allure.step(
                "Verify the notifier now contains the Allure JUnit 4 listener",
                () -> assertThat(getListeners(notifier)).anyMatch(AllureJunit4.class::isInstance)
        );
    }

    @Test
    void shouldSkipDerivedRunNotifierInstances() throws Exception {
        final DerivedRunNotifier notifier = new DerivedRunNotifier();
        final JoinPoint point = mock(JoinPoint.class);
        when(point.getThis()).thenReturn(notifier);

        new AllureJunit4ListenerAspect().addListener(point);

        assertThat(getListeners(notifier))
                .noneMatch(AllureJunit4.class::isInstance);
    }

    @SuppressWarnings("unchecked")
    private static List<RunListener> getListeners(final RunNotifier notifier) throws Exception {
        final Field listeners = RunNotifier.class.getDeclaredField("listeners");
        listeners.setAccessible(true);
        return (List<RunListener>) listeners.get(notifier);
    }

    private static final class TrackingRunner extends Runner implements Filterable {
        private final boolean throwNoTestsRemain;
        private Filter appliedFilter;

        private TrackingRunner(final boolean throwNoTestsRemain) {
            this.throwNoTestsRemain = throwNoTestsRemain;
        }

        @Override
        public Description getDescription() {
            return Description.EMPTY;
        }

        @Override
        public void run(final RunNotifier notifier) {
            // no-op
        }

        @Override
        public void filter(final Filter filter) throws NoTestsRemainException {
            this.appliedFilter = filter;
            if (throwNoTestsRemain) {
                throw new NoTestsRemainException();
            }
        }
    }

    private static final class DerivedRunNotifier extends RunNotifier {
    }
}
