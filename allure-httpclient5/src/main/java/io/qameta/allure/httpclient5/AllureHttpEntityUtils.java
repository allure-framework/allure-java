/*
 *  Copyright 2016-2024 Qameta Software Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.httpclient5;

import io.qameta.allure.AllureResultsWriteException;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

/**
 * Utility class for working with HTTP entity in Allure framework.
 */
@SuppressWarnings({"checkstyle:ParameterAssignment", "PMD.AssignmentInOperand"})
public final class AllureHttpEntityUtils {

    private AllureHttpEntityUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Retrieves the body of the HTTP entity as a string.
     *
     * @param httpEntity the HTTP entity
     * @return the body of the HTTP entity as a string
     * @throws AllureResultsWriteException if an error occurs while reading the entity body
     */
    static String getBody(final HttpEntity httpEntity) {
        try {
            final String contentEncoding = httpEntity.getContentEncoding();
            if (contentEncoding != null && contentEncoding.contains("gzip")) {
                return unpackGzipEntityString(httpEntity);
            } else {
                return EntityUtils.toString(httpEntity, getContentEncoding(httpEntity.getContentEncoding()));
            }
        } catch (IOException | ParseException e) {
            throw new AllureResultsWriteException("Can't read request message body to String", e);
        }
    }

    /**
     * Retrieves the content encoding of the HTTP entity.
     *
     * @param contentEncoding the content encoding value
     * @return the charset corresponding to the content encoding, or UTF-8 if the encoding is invalid
     */
    static Charset getContentEncoding(final String contentEncoding) {
        try {
            return Charset.forName(contentEncoding);
        } catch (IllegalArgumentException ignored) {
            return StandardCharsets.UTF_8;
        }
    }

    /**
     * Unpacks the GZIP-encoded entity string.
     *
     * @param entity the GZIP-encoded HTTP entity
     * @return the unpacked entity string
     * @throws IOException if an error occurs while unpacking the entity
     */
    static String unpackGzipEntityString(final HttpEntity entity) throws IOException {
        final GZIPInputStream gis = new GZIPInputStream(entity.getContent());
        final Charset contentEncoding = getContentEncoding(entity.getContentEncoding());
        try (InputStreamReader inputStreamReader = new InputStreamReader(gis, contentEncoding)) {
            try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                final StringBuilder outStr = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    outStr.append(line);
                }
                return outStr.toString();
            }
        }
    }

}
