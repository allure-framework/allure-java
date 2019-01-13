package io.qameta.allure.scalatest.testdata

import org.scalatest.FlatSpec
import org.scalatest.Matchers._

/**
  * @author charlie (Dmitry Baev).
  */
class CancelledSpec extends FlatSpec {

  "test" should "be cancelled" in {
    cancel("hell yes")
  }

}
