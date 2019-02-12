/*
 *  Copyright 2019 Qameta Software OÜ
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
package io.qameta.allure.junit4.aspect;

import io.qameta.allure.junit4.AllureJunit4;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.junit.runner.notification.RunNotifier;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
@Aspect
public class AllureJunit4ListenerAspect {

    private final AllureJunit4 allure = new AllureJunit4();

    @After("execution(org.junit.runner.notification.RunNotifier.new())")
    public void addListener(final JoinPoint point) {
        final RunNotifier notifier = (RunNotifier) point.getThis();
        notifier.removeListener(allure);
        notifier.addListener(allure);
    }

}
