package io.qameta.allure.junitplatform.features;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author jkttt on 05.07.17.
 */
@Tag(TaggedTests.CLASS_TAG)
public class TaggedTests {

    public static final String CLASS_TAG = "class_tag";
    public static final String METHOD_TAG = "single_tag";

    @Test
    @Tag(METHOD_TAG)
    void taggedTest() {}
}
