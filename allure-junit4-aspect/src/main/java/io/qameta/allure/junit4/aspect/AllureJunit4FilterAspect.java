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
package io.qameta.allure.junit4.aspect;

import io.qameta.allure.junit4.AllureJunit4Filter;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
@Aspect
public class AllureJunit4FilterAspect {

    @SuppressWarnings("PMD.SystemPrintln")
    @AfterReturning(
            value = "execution(public org.junit.runner.Runner org.junit.runner.Request+.getRunner())",
            returning = "runner"
    )
    public void filterBeforeRun(final Runner runner) {
        if (runner instanceof Filterable) {
            try {
                ((Filterable) runner).filter(new AllureJunit4Filter());
            } catch (NoTestsRemainException ignored) {
                //do nothing
            }
        }
    }

}
