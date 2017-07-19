package io.qameta.allure.junit4.samples;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author gladnik (Nikolai Gladkov)
 */
public class IgnoredTests {

    @Test
    @Ignore
    public void ignoredTest() {
    }

    @Test
    @Ignore("Ignored for some reason")
    public void ignoredWithDescriptionTest() {
    }
}
