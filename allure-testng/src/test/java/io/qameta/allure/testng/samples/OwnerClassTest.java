package io.qameta.allure.testng.samples;

import io.qameta.allure.Owner;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
@Owner("eroshenkoam")
public class OwnerClassTest {

    @Test
    @Owner("other-guy")
    public void testWithOwner() throws Exception {
    }

    @Test
    public void testWithoutOwner() throws Exception {
    }
}
