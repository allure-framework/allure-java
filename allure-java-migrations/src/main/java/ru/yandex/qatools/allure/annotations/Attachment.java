package ru.yandex.qatools.allure.annotations;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  A file with additional information captured during a test such
 *  as log, screenshot, log file, dump, server response and so on.
 *  When some test fails attachments help to understand the reason
 *  of failure faster.
 *  <p/>
 *  An attachment is simply a method annotated with
 *  {@link ru.yandex.qatools.allure.annotations.Attachment} returns
 *  either String or byte array which should be added to report:
 *  <p/>
 *  <pre>
 *  &#064;Attachment(value = "Page screenshot", type = "image/png")
 *  public byte[] saveScreenshot(byte[] screenShot) {
 *      return screenShot;
 *  }
 *  </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Attachment {

    /**
     *  Attachment name.
     */
    String value() default "{method}";

    /**
     * Valid attachment MimeType, for example "text/plain" or "application/json".
     */
    String type() default "";
}
