/*
 *  Copyright 2016-2024 Qameta Software Inc
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
package io.qameta.allure.internal;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;

/**
 * Storage that stores information about not finished tests and steps.
 *
 * @author charlie (Dmitry Baev).
 */
public class AllureThreadContext {

    private final Context context = new Context();

    /**
     * Returns last (most recent) uuid.
     */
    public Optional<String> getCurrent() {
        final LinkedList<String> uuids = context.get();
        return uuids.isEmpty()
                ? Optional.empty()
                : Optional.of(uuids.getFirst());
    }

    /**
     * Returns first (oldest) uuid.
     */
    public Optional<String> getRoot() {
        final LinkedList<String> uuids = context.get();
        return uuids.isEmpty()
                ? Optional.empty()
                : Optional.of(uuids.getLast());
    }

    /**
     * Adds new uuid.
     */
    public void start(final String uuid) {
        Objects.requireNonNull(uuid, "step uuid");
        context.get().push(uuid);
    }

    /**
     * Removes latest added uuid. Ignores empty context.
     *
     * @return removed uuid.
     */
    public Optional<String> stop() {
        final LinkedList<String> uuids = context.get();
        if (!uuids.isEmpty()) {
            return Optional.of(uuids.pop());
        }
        return Optional.empty();
    }

    /**
     * Removes all the data stored for current thread.
     */
    public void clear() {
        context.remove();
    }

    /**
     * Thread local context that stores information about not finished tests and steps.
     */
    private static final class Context extends InheritableThreadLocal<LinkedList<String>> {

        @Override
        public LinkedList<String> initialValue() {
            return new LinkedList<>();
        }

        @Override
        protected LinkedList<String> childValue(final LinkedList<String> parentStepContext) {
            return new LinkedList<>(parentStepContext);
        }

    }
}
