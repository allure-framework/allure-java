package io.qameta.allure.scalatest.testdata

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

/**
  * @author charlie (Dmitry Baev).
  */
class BrokenSpec extends FlatSpec {

  "test" should "be failed" in {
    throw new RuntimeException("hell no")
  }

}
