package io.qameta.allure.scalatest.testdata

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

/**
  * @author charlie (Dmitry Baev).
  */
class FailedSpec extends FlatSpec {

  "test" should "be failed" in {
    "hello" shouldBe "hell no"
  }

}
