package io.qameta.allure.cucumber2jvm;

import io.qameta.allure.model.Status;

/**
 * Parent hook type for Before and After.
 */
class ParentHook {
    protected final String uuid;
    private Status status = Status.PASSED;

    ParentHook(final String uuid) {
        this.uuid = uuid;
    }

    /**
     * Update status only if previous is PASSED.
     * @param status status to update
     */
    protected void updateStatus(final Status status) {
        if (this.status == Status.PASSED) {
            this.status = status;
        }
    }

    /**
     * Get current status. Default is PASSED
     */
    protected Status getStatus() {
        return status;
    }
}
