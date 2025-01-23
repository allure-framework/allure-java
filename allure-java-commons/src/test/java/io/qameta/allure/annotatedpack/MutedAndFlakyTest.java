package io.qameta.allure.annotatedpack;

import io.qameta.allure.Flaky;
import io.qameta.allure.Muted;

@Muted
@Flaky
public class MutedAndFlakyTest {

    @Muted
    @Flaky
    public void mutedAndFlakyMethod() {

    }
}
