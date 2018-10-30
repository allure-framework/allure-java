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
    private static class Context extends InheritableThreadLocal<LinkedList<String>> {

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
