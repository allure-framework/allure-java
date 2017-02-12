package io.qameta.allure.storages;

import io.qameta.allure.model.Status;
import io.qameta.allure.model.TestStepResult;

import java.util.Deque;
import java.util.LinkedList;

public class StepStorage extends InheritableThreadLocal<Deque<TestStepResult>> {

    public static final Object LOCK = new Object();

    /**
     * Returns the current thread's "initial value". Construct an new
     * {@link java.util.Deque} with root step {@link #createRootStep()}
     *
     * @return the initial value for this thread-local
     */
    @Override
    protected Deque<TestStepResult> initialValue() {
        Deque<TestStepResult> queue = new LinkedList<>();
        queue.add(createRootStep());
        return queue;
    }

    /**
     * In case parent thread spawn thread we need create a new queue
     * for child thread but use the only one root step. In the end all steps will be
     * children of root step, all we need is sync adding steps
     *
     * @param parentValue value from parent thread
     * @return local copy of queue in this thread with parent root as first element
     */
    @Override
    protected Deque<TestStepResult> childValue(Deque<TestStepResult> parentValue) {
        Deque<TestStepResult> queue = new LinkedList<>();
        queue.add(parentValue.getFirst());
        return queue;
    }

    /**
     * Retrieves, but does not remove, the last element of this deque.
     *
     * @return the tail of this deque
     */
    public TestStepResult getLast() {
        return get().getLast();
    }

    /**
     * Inserts the specified element into the queue represented by this deque
     *
     * @param step the element to add
     */
    public TestStepResult put(TestStepResult step) {
        synchronized (LOCK) {
            get().add(step);
        }
        return step;
    }

    /**
     * Removes the last element of deque in the current thread's copy of this
     * thread-local variable. If after this deque is empty add new root step
     * {@link #createRootStep()}
     *
     * @return the element removed from deque
     */
    public TestStepResult pollLast() {
        Deque<TestStepResult> queue = get();
        TestStepResult last = queue.pollLast();
        if (queue.isEmpty()) {
            queue.add(createRootStep());
        }
        return last;
    }

    /**
     * Move last step to children of previous step. How it works:
     * <pre>
     * before: step1(...) -> step2(child1 -> ... -> childN) -> step3(...) -> ... -> null
     * after:  step2(child1 -> ... -> childN -> step1(...)) -> step3(...) -> ... -> null
     * </pre>
     *
     * @return ex-last step
     */
    public TestStepResult adopt() {
        TestStepResult step = pollLast();
        synchronized (LOCK) {
            getLast().getSteps().add(step);
        }
        return step;
    }

    /**
     * Construct new root step. Used for inspect problems with AllureOld lifecycle
     *
     * @return new root step marked as broken
     */
    public TestStepResult createRootStep() {
        return new TestStepResult()
                .withName("AllureOld step processing error: if you see this step something went wrong.")
                .withStart(System.currentTimeMillis())
                .withStatus(Status.BROKEN);
    }
}
