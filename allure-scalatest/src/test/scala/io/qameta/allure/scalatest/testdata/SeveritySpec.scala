package io.qameta.allure.scalatest.testdata

import io.qameta.allure.{Severity, SeverityLevel}
import org.scalatest.FlatSpec

/**
  * @author charlie (Dmitry Baev).
  */
@Severity(SeverityLevel.BLOCKER)
class SeveritySpec extends FlatSpec {

  "test" should "be passed" in {
  }

}
