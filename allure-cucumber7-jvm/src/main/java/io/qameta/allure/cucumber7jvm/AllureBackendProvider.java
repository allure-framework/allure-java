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
package io.qameta.allure.cucumber7jvm;

import io.cucumber.core.backend.Backend;
import io.cucumber.core.backend.BackendProviderService;
import io.cucumber.core.backend.Container;
import io.cucumber.core.backend.Glue;
import io.cucumber.core.backend.HookDefinition;
import io.cucumber.core.backend.Lookup;
import io.cucumber.core.backend.Snippet;
import io.cucumber.core.backend.TestCaseState;
import org.opentest4j.TestAbortedException;

import java.net.URI;
import java.util.List;
import java.util.function.Supplier;

/**
 * Automatically installs the native pre-scenario muted-test guard.
 */
public class AllureBackendProvider implements BackendProviderService {

    @Override
    public Backend create(final Lookup lookup, final Container container,
                          final Supplier<ClassLoader> classLoader) {
        return new MutedBackend();
    }

    private static final class MutedBackend implements Backend {

        private final AllureTestPlan plan = new AllureTestPlan();

        @Override
        public void loadGlue(final Glue glue, final List<URI> gluePaths) {
            if (!plan.hasMutedTests()) {
                return;
            }
            glue.addBeforeHook(new HookDefinition() {
                @Override
                public void execute(final TestCaseState state) {
                    if (plan.isMuted(state.getSourceTagNames(), state.getUri(), state.getLine())) {
                        throw new TestAbortedException("Skipped by Allure test plan");
                    }
                }

                @Override
                public String getTagExpression() {
                    return "";
                }

                @Override
                public int getOrder() {
                    return Integer.MIN_VALUE;
                }

                @Override
                public boolean isDefinedAt(final StackTraceElement element) {
                    return false;
                }

                @Override
                public String getLocation() {
                    return AllureBackendProvider.class.getName();
                }
            });
        }

        @Override
        public void buildWorld() {
            // The hook requires no scenario-scoped world.
        }

        @Override
        public void disposeWorld() {
            // The hook owns no scenario resources.
        }

        @Override
        public Snippet getSnippet() {
            // Reporting backends do not supply step-definition snippets.
            return null;
        }
    }
}
