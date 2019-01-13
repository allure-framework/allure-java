package io.qameta.allure.scalatest

import io.qameta.allure.model.Stage.FINISHED
import io.qameta.allure.model.Status
import io.qameta.allure.scalatest.testdata._
import io.qameta.allure.test.{AllureResults, AllureResultsWriterStub}
import io.qameta.allure.{Allure, AllureLifecycle}
import org.junit.jupiter.api.Test
import org.scalatest.Matchers._
import org.scalatest.tools.Runner

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

/**
  * @author charlie (Dmitry Baev).
  */
class AllureScalatestTest {

  @Test
  def shouldSetName(): Unit = {
    val results = run(classOf[SimpleSpec])
    asScalaBuffer(results.getTestResults)
      .map(item => item.getName) should contain("test should be passed")
  }

  @Test
  def shouldSetStart(): Unit = {
    val results = run(classOf[SimpleSpec])

    val starts = asScalaBuffer(results.getTestResults)
      .map(item => item.getStart).toList

    every(starts) should not be null
  }

  @Test
  def shouldSetStop(): Unit = {
    val results = run(classOf[SimpleSpec])

    val stops = asScalaBuffer(results.getTestResults)
      .map(item => item.getStop)

    every(stops) should not be null
  }

  @Test
  def shouldSetStage(): Unit = {
    val results = run(classOf[SimpleSpec])

    val stages = asScalaBuffer(results.getTestResults)
      .map(item => item.getStage)

    every(stages) shouldBe FINISHED
  }

  @Test
  def shouldSetStatus(): Unit = {
    val results = run(classOf[SimpleSpec])

    val statuses = asScalaBuffer(results.getTestResults)
      .map(item => item.getStatus)

    every(statuses) shouldBe Status.PASSED
  }

  @Test
  def shouldSetFailedStatus(): Unit = {
    val results = run(classOf[FailedSpec])

    results.getTestResults should have length 1

    val statuses = asScalaBuffer(results.getTestResults)
      .map(item => item.getStatus)

    every(statuses) shouldBe Status.FAILED
  }

  @Test
  def shouldSetBrokenStatus(): Unit = {
    val results = run(classOf[BrokenSpec])

    results.getTestResults should have length 1

    val statuses = asScalaBuffer(results.getTestResults)
      .map(item => item.getStatus).toList

    every(statuses) should be(Status.BROKEN)
  }

  @Test
  def shouldSetSkippedStatus(): Unit = {
    val results = run(classOf[CancelledSpec])

    results.getTestResults should have length 1

    val statuses = asScalaBuffer(results.getTestResults)
      .map(item => item.getStatus).toList

    every(statuses) should be(Status.SKIPPED)
  }

  @Test
  def shouldProcessSuiteAnnotations(): Unit = {
    val results = run(classOf[AnnotationsOnClassSpec])

    results.getTestResults should have length 1

    val labels = asScalaBuffer(results.getTestResults)
      .flatMap(item => asScalaBuffer(item.getLabels))
      .map(label => (label.getName, label.getValue))
      .toList

    labels should contain(("epic", "E1"))
    labels should contain(("feature", "F1"))
    labels should contain(("story", "S1"))
    labels should contain(("owner", "charlie"))
  }

  private def run(clazz: Class[_]): AllureResults = {
    val args = new ListBuffer[String]
    args += "-s"
    args += clazz.getCanonicalName
    args += "-C"
    args += classOf[AllureScalatest].getCanonicalName

    val writer = new AllureResultsWriterStub
    val lifecycle = new AllureLifecycle(writer)
    val defaultLifecycle = Allure.getLifecycle
    try {
      Allure.setLifecycle(lifecycle)
      Runner.run(args.toArray)
    } catch {
      case _: Throwable => ()
    } finally {
      Allure.setLifecycle(defaultLifecycle)
    }
    writer
  }

}
