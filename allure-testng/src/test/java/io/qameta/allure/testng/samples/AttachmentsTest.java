package io.qameta.allure.testng.samples;

import io.qameta.allure.Attachment;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author charlie (Dmitry Baev).
 */
public class AttachmentsTest {

    @Test
    public void testWithAttachment() {
        attachment();
        Assert.assertTrue(true);
    }

    @Attachment(value = "String attachment", type = "text/plain")
    public String attachment() {
        return "<p>HELLO</p>";
    }

}
