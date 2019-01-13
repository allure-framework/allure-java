package io.qameta.allure.scalatest

import java.lang.annotation.Annotation
import java.util.UUID

import io.qameta.allure._
import io.qameta.allure.model.{Stage, Status, StatusDetails, TestResult}
import io.qameta.allure.util.ResultsUtils._
import org.scalatest.Reporter
import org.scalatest.events._
import org.scalatest.exceptions.TestFailedException

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * @author charlie (Dmitry Baev).
  */
class AllureScalatest(val lifecycle: AllureLifecycle) extends Reporter {

  private val suites = mutable.HashMap[String, Location]()

  def this() = this(Allure.getLifecycle)

  override def apply(event: Event): Unit = event match {
    case event: SuiteStarting => startSuite(event)
    case event: SuiteCompleted => completeSuite(event)
    case event: SuiteAborted => abortSuite(event)
    case event: TestStarting => startTestCase(event)
    case event: TestFailed => failTestCase(event)
    case event: TestCanceled => cancelTestCase(event)
    case event: TestSucceeded => passTestCase(event)
    case _ => ()
  }

  def startSuite(event: SuiteStarting): Unit = {
    event.location.fold {} { location => suites += event.suiteId -> location }
  }

  def completeSuite(event: SuiteCompleted): Unit = {
    suites -= event.suiteId
  }

  def abortSuite(event: SuiteAborted): Unit = {
    suites -= event.suiteId
  }

  def startTestCase(event: TestStarting): Unit = {
    val uuid = UUID.randomUUID().toString
    var labels = mutable.ListBuffer(
      createSuiteLabel(event.suiteName),
      createThreadLabel(),
      createHostLabel(),
      createLanguageLabel("scala"),
      createFrameworkLabel("scalatest")
    )
    labels ++= asScalaSet(getProvidedLabels)

    var links = mutable.ListBuffer[io.qameta.allure.model.Link]()

    val result = new TestResult()
      .setName(event.testName)
      .setUuid(uuid)

    val testAnnotations = getAnnotations(event.location)
    val suiteAnnotations = getAnnotations(suites.get(event.suiteId))

    (testAnnotations ::: suiteAnnotations).foreach {
      case annotation: Severity => labels += createSeverityLabel(annotation.value())
      case annotation: Owner => labels += createOwnerLabel(annotation.value())
      case annotation: Description => result.setDescription(annotation.value())
      case annotation: Epic => labels += createEpicLabel(annotation.value())
      case annotation: Feature => labels += createFeatureLabel(annotation.value())
      case annotation: Story => labels += createStoryLabel(annotation.value())
      case annotation: Link => links += createLink(annotation)
      case annotation: Issue => links += createIssueLink(annotation.value())
      case annotation: TmsLink => links += createTmsLink(annotation.value())
      case _ => None
    }

    event.suiteClassName.map(className => createTestClassLabel(className))
      .fold {} { value => labels += value }

    result.setLabels(labels.asJava)

    lifecycle.scheduleTestCase(result)
    lifecycle.startTestCase(uuid)
  }

  def failTestCase(event: TestFailed): Unit = {
    val throwable = event.throwable.getOrElse(new RuntimeException(event.message))
    val status = throwable match {
      case _: TestFailedException => Status.FAILED
      case _ => Status.BROKEN
    }
    val statusDetails = getStatusDetails(throwable)
      .orElse(new StatusDetails().setMessage(event.message))

    lifecycle.getCurrentTestCase.ifPresent(uuid => {
      lifecycle.updateTestCase(uuid, (result: TestResult) => {
        result.setStatus(status)
        result.setStatusDetails(statusDetails)
        result.setStage(Stage.FINISHED)
      }: Unit)
      lifecycle.stopTestCase(uuid)
      lifecycle.writeTestCase(uuid)
    })
  }

  def passTestCase(event: TestSucceeded): Unit = {
    lifecycle.getCurrentTestCase.ifPresent(uuid => {
      lifecycle.updateTestCase(uuid, (result: TestResult) => {
        result.setStatus(Status.PASSED)
        result.setStage(Stage.FINISHED)
      }: Unit)
      lifecycle.stopTestCase(uuid)
      lifecycle.writeTestCase(uuid)
    })
  }

  def cancelTestCase(event: TestCanceled): Unit = {
    lifecycle.getCurrentTestCase.ifPresent(uuid => {
      lifecycle.updateTestCase(uuid, (result: TestResult) => {
        result.setStatus(Status.SKIPPED)
        result.setStage(Stage.FINISHED)
          .setStatusDetails(new StatusDetails().setMessage(event.message))
      }: Unit)
      lifecycle.stopTestCase(uuid)
      lifecycle.writeTestCase(uuid)
    })
  }

  private def getAnnotations(location: Option[Location]): List[Annotation] = location match {
    case Some(TopOfClass(className)) => Class.forName(className).getAnnotations.toList
    case Some(TopOfMethod(className, methodName)) => Class.forName(className).getMethod(methodName).getDeclaredAnnotations.toList
    case _ => List()
  }

  private class SuiteInfo {

  }

}
