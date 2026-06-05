/*
 *  Copyright 2016-2026 Qameta Software Inc
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
package io.qameta.allure.servletapi;

import io.qameta.allure.Allure;
import io.qameta.allure.http.HttpExchangeRequest;
import io.qameta.allure.http.HttpExchangeResponse;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpServletAttachmentBuilderTest {

    @Test
    void shouldBuildRequestWithHeadersCookiesAndBody() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/orders");
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of("X-Trace", "Accept")));
        when(request.getHeader("X-Trace")).thenReturn("trace-1");
        when(request.getHeader("Accept")).thenReturn("application/json");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("session", "abc123")});
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{\"ok\":true}")));

        final HttpExchangeRequest attachment = HttpServletAttachmentBuilder.buildRequest(request);

        assertEquals("POST", attachment.method());
        assertEquals("/orders", attachment.url());
        assertEquals("{\"ok\":true}", attachment.body().value());
        assertEquals(
                List.of("X-Trace=trace-1", "Accept=application/json"), attachment.headers().stream()
                        .map(header -> header.name() + "=" + header.value())
                        .toList()
        );
        assertEquals(
                List.of("session=abc123"), attachment.cookies().stream()
                        .map(cookie -> cookie.name() + "=" + cookie.value())
                        .toList()
        );
    }

    @Test
    void shouldHandleRequestsWithoutCookies() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/orders");
        when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(request.getCookies()).thenReturn(null);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("")));

        final HttpExchangeRequest attachment = Allure.step(
                "Build a request attachment when the servlet container returns null cookies",
                () -> assertDoesNotThrow(() -> HttpServletAttachmentBuilder.buildRequest(request))
        );

        Allure.step(
                "Verify the request attachment omits the empty cookie list", () -> assertNull(attachment.cookies())
        );
    }

    @Test
    void shouldBuildResponseWithHeaders() {
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getStatus()).thenReturn(200);
        when(response.getHeaderNames()).thenReturn(List.of("Content-Type"));
        when(response.getHeaders("Content-Type")).thenReturn(List.of("application/json"));

        final HttpExchangeResponse attachment = HttpServletAttachmentBuilder.buildResponse(response);

        assertEquals(200, attachment.status());
        assertEquals(
                List.of("Content-Type=application/json"), attachment.headers().stream()
                        .map(header -> header.name() + "=" + header.value())
                        .toList()
        );
    }

    @Test
    void shouldReturnEmptyBodyWhenRequestReaderFails() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getReader()).thenThrow(new IOException("boom"));

        final String body = Allure.step(
                "Read the request body when the servlet reader throws", () -> HttpServletAttachmentBuilder.getBody(request)
        );

        Allure.step("Verify the fallback body is empty", () -> assertEquals("", body));
    }
}
