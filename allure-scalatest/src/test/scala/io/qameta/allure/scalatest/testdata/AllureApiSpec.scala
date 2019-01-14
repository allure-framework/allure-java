package io.qameta.allure.scalatest.testdata

import io.qameta.allure.Allure.{StepContext, step}
import io.qameta.allure.scalatest.AllureScalatestContext
import org.scalatest.FlatSpec

/**
  * @author charlie (Dmitry Baev).
  */
class AllureApiSpec extends FlatSpec {

  "test" should "be passed" in new AllureScalatestContext {
    step("first")
    step("second", () => {
      step("child1")
      step("child2")
      step("child3")
      Unit
    })
    step("third", (context: StepContext) => {
      val a = context.parameter("a", 123L)
      val b = context.parameter("b", "hello")
      Unit
    })
  }

}
