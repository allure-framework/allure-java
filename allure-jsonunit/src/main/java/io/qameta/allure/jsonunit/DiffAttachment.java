package io.qameta.allure.jsonunit;

import io.qameta.allure.attachment.AttachmentData;

/**
 * @author Victor Orlovsky
 */
public class DiffAttachment implements AttachmentData {

    private final String patch;
    private final String actual;
    private final String expected;

    public DiffAttachment(final String actual, final String expected, final String patch) {
        this.actual = actual;
        this.expected = expected;
        this.patch = patch;
    }

    public String getPatch() {
        return patch;
    }

    public String getActual() {
        return actual;
    }

    public String getExpected() {
        return expected;
    }

    @Override
    public String getName() {
        return "JSON difference";
    }
}
