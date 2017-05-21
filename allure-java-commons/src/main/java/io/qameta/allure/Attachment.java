package io.qameta.allure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to mark methods that produce attachments. Returned value of such methods
 * will be copied and shown in the report as attachment.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Attachment {

    /**
     * The attachment name.
     *
     * @return the attachment name.
     */
    String value() default "";

    /**
     * Valid attachment MimeType, for example "text/plain" or "application/json".
     *
     * @return the attachment type.
     */
    String type() default "";

    /**
     * Optional attachment file extension. By default file extension will be determined by
     * provided media type. Should be started with dot.
     *
     * @return the attachment file extension.
     */
    String fileExtension() default "";
}
