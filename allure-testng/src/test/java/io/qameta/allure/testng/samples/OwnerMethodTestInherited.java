package io.qameta.allure.testng.samples;

/**
 * @author charlie (Dmitry Baev).
 */
public class OwnerMethodTestInherited extends OwnerMethodTest {

    @Override
    public void testWithOwner() throws Exception {
        super.testWithOwner();
    }

    @Override
    public void testWithoutOwner() throws Exception {
        super.testWithoutOwner();
    }
}
