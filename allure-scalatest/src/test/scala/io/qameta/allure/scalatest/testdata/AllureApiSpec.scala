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
package io.qameta.allure.scalatest.testdata

import io.qameta.allure.Allure.{StepContext, step}
import io.qameta.allure.scalatest.AllureScalatestContext
import org.scalatest.flatspec.AnyFlatSpec

/**
  * @author charlie (Dmitry Baev).
  */
class AllureApiSpec extends AnyFlatSpec {

  "test" should "be passed" in new AllureScalatestContext {
    step("first")
    step("second", () => {
      step("child1")
      step("child2")
      step("child3")
      () =>
    })
    step("third", (context: StepContext) => {
      val a = context.parameter("a", 123L)
      val b = context.parameter("b", "hello")
      () =>
    })
  }

}
