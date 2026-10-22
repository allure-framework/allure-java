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
package io.qameta.allure.selenide;

import com.codeborne.selenide.impl.Photographer;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.util.Optional;

/**
 * Test fixture for {@link AllureSelenideTest}, not a test class itself.
 *
 * <p>Registered globally via {@code META-INF/services/com.codeborne.selenide.impl.Photographer},
 * so it delegates to the driver (same as Selenide's default photographer) unless
 * {@link #screenshot} is set by a test.</p>
 */
public class TestPhotographer implements Photographer {

    static byte[] screenshot;

    @Override
    public <T> Optional<T> takeScreenshot(final WebDriver webDriver, final OutputType<T> outputType) {
        if (screenshot != null) {
            return Optional.of(outputType.convertFromPngBytes(screenshot));
        }
        return webDriver instanceof TakesScreenshot
                ? Optional.ofNullable(((TakesScreenshot) webDriver).getScreenshotAs(outputType))
                : Optional.empty();
    }
}

