package io.qameta.allure.testng.samples;

import io.qameta.allure.Step;
import io.qameta.allure.testng.beans.Bean;
import org.testng.annotations.Test;

/**
 * Author: Sergey Potanin
 * Date: 20/08/2018
 */
public class TestWithStepsWithParameters {

    @Test
    public void test() {
        Bean bean = new Bean("expected");
        stepWithFieldNotExists(bean);
        stepWithFieldExists(bean);
    }

    @Step("Field exists {bean.exists}")
    private void stepWithFieldExists(@SuppressWarnings("unused") Bean bean) {

    }

    @Step("Field not exists {bean.notExists}")
    private void stepWithFieldNotExists(@SuppressWarnings("unused") Bean bean) {

    }
}
