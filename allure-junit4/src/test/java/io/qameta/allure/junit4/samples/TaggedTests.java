package io.qameta.allure.junit4.samples;

import io.qameta.allure.junit4.Tag;
import io.qameta.allure.junit4.Tags;
import org.junit.Test;

/**
 * @author jkttt on 05.07.17.
 */
@Tags({@Tag(TaggedTests.CLASS_TAG1), @Tag(TaggedTests.CLASS_TAG2)})
public class TaggedTests {

    public static final String METHOD_TAG2 = "method_tag1";
    public static final String METHOD_TAG1 = "method_tag2";
    public static final String CLASS_TAG1 = "class_tag1";
    public static final String CLASS_TAG2 = "class_tag2";

    @Test
    @Tags({@Tag(METHOD_TAG1),
            @Tag(METHOD_TAG2)})
    public void taggedTest() {}
}
