package io.qameta.allure.testng.samples;

import io.qameta.allure.Owner;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class OwnerMethodTest {

    @Test
    @Owner("charlie")
    public void testWithOwner() throws Exception {
    }

    @Test
    public void testWithoutOwner() throws Exception {
    }
}
