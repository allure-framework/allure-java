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
package io.qameta.allure.junit4.samples;

import io.qameta.allure.junit4.DisplayName;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

import java.lang.annotation.Annotation;

@RunWith(TestBasedOnSampleRunner.SampleRunnerBasedOnNotClasses.class)
public class TestBasedOnSampleRunner {

    public static class SampleRunnerBasedOnNotClasses extends Runner {
        @SuppressWarnings("unused")
        public SampleRunnerBasedOnNotClasses(Class testClass) {
            super();
        }

        @Override
        public Description getDescription() {
            return Description.createTestDescription(
                    "allure junit4 runner.test for non-existing classes (would be a class in normal runner)",
                    "should correctly handle non-existing classes (would be method name in normal runner)",
                    new DisplayName() {
                        @Override
                        public String value() {
                            return "Some human readable name";
                        }

                        @Override
                        public Class<? extends Annotation> annotationType() {
                            return DisplayName.class;
                        }
                    }
            );
        }

        @Override
        public void run(RunNotifier notifier) {
            notifier.fireTestStarted(getDescription());
            notifier.fireTestFinished(getDescription());
        }
    }
}
