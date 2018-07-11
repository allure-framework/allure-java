package io.qameta.allure.cucumber3jvm;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Plugin utils.
 */
final class Utils {
    private static final String MD_5 = "md5";

    private Utils() {
    }

    public static String md5(final String source) {
        final byte[] bytes = getMessageDigest().digest(source.getBytes(UTF_8));
        return new BigInteger(1, bytes).toString(16);
    }

    private static MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance(MD_5);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find md5 hashing algorithm", e);
        }
    }

}
