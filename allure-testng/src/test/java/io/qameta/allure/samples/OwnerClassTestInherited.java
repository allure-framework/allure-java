package io.qameta.allure.samples;

/**
 * @author charlie (Dmitry Baev).
 */
public class OwnerClassTestInherited extends OwnerClassTest {

    @Override
    public void testWithOwner() throws Exception {
        super.testWithOwner();
    }

    @Override
    public void testWithoutOwner() throws Exception {
        super.testWithoutOwner();
    }

}
