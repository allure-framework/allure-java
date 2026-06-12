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
package io.qameta.allure.seleniumbidi;

import io.qameta.allure.Description;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.BiDiException;
import org.openqa.selenium.bidi.HasBiDi;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeleniumBiDiSessionFactoryTest {

    /**
     * Verifies Selenium 4.44 lazy BiDi initialization support.
     *
     * <p>Selenium's augmented remote driver can implement {@link HasBiDi} while returning
     * {@link Optional#empty()} from {@code maybeGetBiDi()} until the BiDi connection is initialized.
     * The session factory must therefore treat the {@link HasBiDi} interface as the availability signal
     * and let Selenium initialize the connection through {@code getBiDi()} when the inspectors start.
     */
    @Description
    @Test
    void shouldInitializeLazyBiDiConnectionFromHasBiDiDriver() {
        final AtomicBoolean getBiDiCalled = new AtomicBoolean();
        final WebDriver driver = lazyBiDiDriver(getBiDiCalled);

        assertThatThrownBy(
                () -> new SeleniumBiDiSessionFactory().start(
                        driver,
                        new BiDiConfiguration(),
                        log -> {
                        },
                        network -> {
                        }
                )
        )
                .isInstanceOf(BiDiException.class)
                .hasMessageContaining("lazy BiDi connection requested");

        assertThat(getBiDiCalled).isTrue();
    }

    private static WebDriver lazyBiDiDriver(final AtomicBoolean getBiDiCalled) {
        return (WebDriver) Proxy.newProxyInstance(
                SeleniumBiDiSessionFactoryTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, HasBiDi.class},
                (proxy, method, args) -> handleDriverMethod(getBiDiCalled, proxy, method, args)
        );
    }

    private static Object handleDriverMethod(final AtomicBoolean getBiDiCalled,
                                             final Object proxy,
                                             final Method method,
                                             final Object[] args) {
        if ("maybeGetBiDi".equals(method.getName())) {
            return Optional.empty();
        }
        if ("getBiDi".equals(method.getName())) {
            getBiDiCalled.set(true);
            throw new BiDiException("lazy BiDi connection requested");
        }
        if ("toString".equals(method.getName())) {
            return "lazy BiDi WebDriver";
        }
        if ("hashCode".equals(method.getName())) {
            return System.identityHashCode(proxy);
        }
        if ("equals".equals(method.getName())) {
            return proxy == args[0];
        }
        throw new UnsupportedOperationException(method.getName());
    }
}
