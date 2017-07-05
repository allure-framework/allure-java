package io.qameta.allure.junit4.samples;

import io.qameta.allure.junit4.Tag;
import io.qameta.allure.junit4.Tags;
import org.junit.Test;

/**
 * @author jkttt on 05.07.17.
 */
public class TaggedTests {

    public static final String METHOD_TAG2 = "method_tag1";
    public static final String METHOD_TAG1 = "method_tag2";
    public static final String CLASS_TAG = "class_tag";

    @Test
    @Tags({@Tag(METHOD_TAG1),
            @Tag(METHOD_TAG2)})
    public void taggedTest() {}
}
