package io.qameta.allure.testng.samples;

import io.qameta.allure.Allure;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class AttachmentsTest {

    @Test
    public void testWithAttachment() {
        Allure.addAttachment("String attachment", "text/plain", "<p>HELLO</p>");
        Assert.assertTrue(true);
    }

}
