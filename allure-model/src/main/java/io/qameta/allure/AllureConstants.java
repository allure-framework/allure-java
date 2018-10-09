package io.qameta.allure;

/**
 * @author @author charlie (Dmitry Baev baev@qameta.io)
 * @since 1.0-BETA1
 */
@SuppressWarnings("unused")
public final class AllureConstants {

    public static final String TEST_RESULT_FILE_SUFFIX = "-result.json";

    public static final String TEST_RESULT_FILE_GLOB = "*-result.json";

    public static final String TEST_RESULT_CONTAINER_FILE_SUFFIX = "-container.json";

    public static final String TEST_RESULT_CONTAINER_FILE_GLOB = "*-container.json";

    public static final String TEST_RUN_FILE_SUFFIX = "-testrun.json";

    public static final String TEST_RUN_FILE_GLOB = "*-testrun.json";

    public static final String ATTACHMENT_FILE_SUFFIX = "-attachment";

    public static final String ATTACHMENT_FILE_GLOB = "*-attachment*";

    private AllureConstants() {
        throw new IllegalStateException("Do not instance");
    }
}
