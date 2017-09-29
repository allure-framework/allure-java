package io.qameta.allure.spock.geb;

import com.google.common.io.Files;

import java.io.File;

/**
 * Supported geb report files.
 *
 * @author Andreas Haardt
 */
public enum GebFileTypes {
    PNG("png", "image/png"),
    HTML("html", "text/html");

    private final String extension;
    private final String type;

    GebFileTypes(final String extension, final String type) {
        this.extension = extension;
        this.type = type;
    }

    public boolean matchExtension(final File file) {
        return this.extension.equals(Files.getFileExtension(file.getName()));
    }

    public static GebFileTypes getFileTypeByFile(final File file) {
        final String extension = Files.getFileExtension(file.getName());
        return GebFileTypes.valueOf(extension.toUpperCase());
    }

    public String getType() {
        return type;
    }

    public String getExtension() {
        return extension;
    }
}
