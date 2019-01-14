package io.qameta.allure.scalatest.testdata

import io.qameta.allure._
import org.scalatest.FunSuite

/**
  * @author charlie (Dmitry Baev).
  */
@Owner("charlie")
@Epic("E1")
@Feature("F1")
@Story("S1")
@Link("https://example.org")
@Issue("https://example.org/issue/1")
@TmsLink("https://example.org/tms/1")
class AnnotationsOnClassSpec extends FunSuite {

  test("demo test") {
  }

}
