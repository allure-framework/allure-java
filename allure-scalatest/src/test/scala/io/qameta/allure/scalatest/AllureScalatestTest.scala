/*
 *  Copyright 2016-2026 Qameta Software Inc
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
package io.qameta.allure.scalatest

import io.qameta.allure.model.Stage.FINISHED
import io.qameta.allure.model.{Stage, Status}
import io.qameta.allure.scalatest.testdata._
import io.qameta.allure.test.IsolatedLifecycle
import io.qameta.allure.test.{AllureResults, AllureResultsWriterStub, RunUtils}
import io.qameta.allure.test.AllureTestCommonsUtils.{attach, expectedHistoryId}
import io.qameta.allure.util.ResultsUtils.createParameter
import io.qameta.allure.{Allure, AllureLifecycle, Description}
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError
import org.scalatest.events.{Event, Ordinal, RunAborted, SuiteAborted}
import org.scalatest.matchers.should.Matchers._
import org.scalatest.tools.Runner

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._
@IsolatedLifecycle
class AllureScalatestTest {

  @Test
  def shouldSetName(): Unit = {
    val results = run(classOf[SimpleSpec])
    results.getTestResults.asScala
      .map(item => item.getName) should contain("test should be passed")
    results.getTestResults.asScala.head.getTitlePath.asScala.toList shouldBe List(
      "io",
      "qameta",
      "allure",
      "scalatest",
      "testdata",
      "SimpleSpec"
    )
  }

  @Test
  def shouldSetStart(): Unit = {
    val results = run(classOf[SimpleSpec])

    val starts = results.getTestResults.asScala
      .map(item => item.getStart)
      .toList

    every(starts) should not be null
  }

  @Test
  def shouldSetStop(): Unit = {
    val results = run(classOf[SimpleSpec])

    val stops = results.getTestResults.asScala
      .map(item => item.getStop)

    every(stops) should not be null
  }

  @Test
  def shouldSetStage(): Unit = {
    val results = run(classOf[SimpleSpec])

    val stages = results.getTestResults.asScala
      .map(item => item.getStage)

    every(stages) shouldBe FINISHED
  }

  @Test
  def shouldSetStatus(): Unit = {
    val results = run(classOf[SimpleSpec])

    val statuses = results.getTestResults.asScala
      .map(item => item.getStatus)

    every(statuses) shouldBe Status.PASSED
    results.getGlobals shouldBe empty
  }

  @Test
  def shouldSetFailedStatus(): Unit = {
    val results = run(classOf[FailedSpec])

    results.getTestResults should have length 1

    val statuses = results.getTestResults.asScala
      .map(item => item.getStatus)

    every(statuses) shouldBe Status.FAILED
    results.getGlobals shouldBe empty
  }

  @Test
  def shouldSetBrokenStatus(): Unit = {
    val results = run(classOf[BrokenSpec])

    results.getTestResults should have length 1

    val statuses = results.getTestResults.asScala
      .map(item => item.getStatus)
      .toList

    every(statuses) should be(Status.BROKEN)
    results.getGlobals shouldBe empty
  }

  @Test
  def shouldSetSkippedStatus(): Unit = {
    val results = run(classOf[CancelledSpec])

    results.getTestResults should have length 1

    val statuses = results.getTestResults.asScala
      .map(item => item.getStatus)
      .toList

    every(statuses) should be(Status.SKIPPED)
    results.getGlobals shouldBe empty
  }

  @Test
  def shouldProcessPendingTests(): Unit = {
    val results = run(classOf[PendingSpec])

    results.getTestResults should have length 1
    val result = results.getTestResults.get(0)
    result.getStatus shouldBe Status.SKIPPED
    result.getStage shouldBe Stage.FINISHED
    result.getTestCaseId should not be empty
    result.getHistoryId should not be empty
  }

  @Test
  def shouldProcessSuiteAnnotations(): Unit = {
    val results = run(classOf[AnnotationsOnClassSpec])

    results.getTestResults should have length 1

    val labels = results.getTestResults.asScala
      .flatMap(item => item.getLabels.asScala)
      .map(label => (label.getName, label.getValue))
      .toList

    labels should contain(("epic", "E1"))
    labels should contain(("feature", "F1"))
    labels should contain(("story", "S1"))
    labels should contain(("owner", "charlie"))
  }

  @Test
  def shouldSetSeverity(): Unit = {
    val results = run(classOf[SeveritySpec])

    results.getTestResults should have length 1

    val labels = results.getTestResults.asScala
      .flatMap(item => item.getLabels.asScala)
      .map(label => (label.getName, label.getValue))
      .toList

    labels should contain(("severity", "blocker"))
  }

  @Test
  def shouldProcessIgnoredTests(): Unit = {
    val results = run(classOf[IgnoredSpec])

    results.getTestResults should have length 1

    results.getTestResults.asScala
      .map(item => item.getName) should contain("test should be ignored")

    val statuses = results.getTestResults.asScala
      .map(item => item.getStatus)
      .toList

    every(statuses) should be(null)

    val stages = results.getTestResults.asScala
      .map(item => item.getStage)
      .toList

    every(stages) should be(Stage.FINISHED)
  }

  @Test
  def shouldSupportJavaApi(): Unit = {
    val results = run(classOf[AllureApiSpec])
    val steps = results.getTestResults.asScala
      .flatMap(item => item.getSteps.asScala)

    steps
      .map(step => step.getName) should contain inOrder ("first", "second", "third")

    steps
      .filter(step => step.getName == "second")
      .flatMap(step => step.getSteps.asScala)
      .map(step => step.getName) should contain inOrder ("child1", "child2", "child3")

  }

  @Test
  def shouldUseRuntimeParametersForHistoryId(): Unit = {
    val originalValue = RuntimeParameterSpec.parameterValue
    try {
      RuntimeParameterSpec.parameterValue = "first"
      val first = run(classOf[RuntimeParameterSpec]).getTestResults.asScala.head

      RuntimeParameterSpec.parameterValue = "second"
      val second = run(classOf[RuntimeParameterSpec]).getTestResults.asScala.head

      first.getTestCaseId should not be empty
      first.getTestCaseId shouldBe second.getTestCaseId
      first.getHistoryId shouldBe expectedHistoryId(
        first.getTestCaseId,
        List(createParameter("runtime", "first")).asJava
      )
      second.getHistoryId shouldBe expectedHistoryId(
        second.getTestCaseId,
        List(createParameter("runtime", "second")).asJava
      )
      first.getHistoryId should not be second.getHistoryId
    } finally {
      RuntimeParameterSpec.parameterValue = originalValue
    }
  }

  @Test
  @Description("A failed beforeAll is reported as a global error with its suite context and exception details.")
  def shouldReportBeforeAllFailureAsGlobalError(): Unit = {
    val started = System.currentTimeMillis()
    val results = run(classOf[BeforeAllFailureSpec])

    results.getGlobals should have length 1
    val errors = results.getGlobals.asScala.flatMap(_.getErrors.asScala)
    errors should have length 1
    val error = errors.head
    error.getMessage should include("BeforeAllFailureSpec")
    error.getMessage should include("beforeAll failed")
    error.getTrace should include("java.lang.IllegalStateException: beforeAll failed")
    error.getTrace should include("BeforeAllFailureSpec.beforeAll")
    error.getTimestamp should be >= started
    error.getTimestamp should be <= System.currentTimeMillis()
  }

  @Test
  @Description("A failed afterAll is reported as a global error with its suite context and exception details.")
  def shouldReportAfterAllFailureAsGlobalError(): Unit = {
    val results = run(classOf[AfterAllFailureSpec])

    results.getGlobals should have length 1
    val errors = results.getGlobals.asScala.flatMap(_.getErrors.asScala)
    errors should have length 1
    val error = errors.head
    error.getMessage should include("AfterAllFailureSpec")
    error.getMessage should include("afterAll failed")
    error.getTrace should include("java.lang.IllegalStateException: afterAll failed")
    error.getTrace should include("AfterAllFailureSpec.afterAll")
  }

  @Test
  @Description("Suite construction failures are reported as run-level global errors.")
  def shouldReportSuiteConstructionFailureAsGlobalError(): Unit = {
    val results = run(classOf[ConstructorFailureSpec])

    results.getGlobals should have length 1
    val errors = results.getGlobals.asScala.flatMap(_.getErrors.asScala)
    errors should have length 1
    val error = errors.head
    error.getMessage should include("ScalaTest run")
    error.getTrace should include("ConstructorFailureSpec")
    error.getTrace should include("suite construction failed")
  }

  @Test
  @Description("A suite abort with no throwable is reported with its message on the supplied lifecycle.")
  def shouldReportSuiteAbortWithoutThrowable(): Unit = {
    val results = report(
      SuiteAborted(
        ordinal = new Ordinal(0),
        message = "suite initialization was interrupted",
        suiteName = "fixture suite",
        suiteId = "fixture-suite",
        suiteClassName = None
      )
    )

    results.getGlobals should have length 1
    val errors = results.getGlobals.asScala.flatMap(_.getErrors.asScala)
    errors should have length 1
    val error = errors.head
    error.getMessage should include("fixture suite")
    error.getMessage should include("suite initialization was interrupted")
    error.getTrace shouldBe null
    error.getTimestamp should be > 0L
  }

  @Test
  @Description("A run abort with no throwable is reported with its message on the supplied lifecycle.")
  def shouldReportRunAbortWithoutThrowable(): Unit = {
    val results = report(
      RunAborted(
        ordinal = new Ordinal(0),
        message = "test discovery was interrupted",
        throwable = None
      )
    )

    results.getGlobals should have length 1
    val errors = results.getGlobals.asScala.flatMap(_.getErrors.asScala)
    errors should have length 1
    val error = errors.head
    error.getMessage should include("ScalaTest run")
    error.getMessage should include("test discovery was interrupted")
    error.getTrace shouldBe null
    error.getTimestamp should be > 0L
  }

  @Test
  @Description("A run abort is reported with its event context, exception details, and comparison values.")
  def shouldReportRunAbortComparisonDetails(): Unit = {
    val cause = new AssertionFailedError("global comparison failed", "expected value", "actual value")
    val results = report(
      RunAborted(
        ordinal = new Ordinal(0),
        message = "ScalaTest initialization failed",
        throwable = Some(cause)
      )
    )

    results.getGlobals should have length 1
    val errors = results.getGlobals.asScala.flatMap(_.getErrors.asScala)
    errors should have length 1
    val error = errors.head
    error.getMessage should include("ScalaTest initialization failed")
    error.getMessage should include("global comparison failed")
    error.getTrace should include("org.opentest4j.AssertionFailedError: global comparison failed")
    error.getExpected shouldBe cause.getExpected.toString
    error.getActual shouldBe cause.getActual.toString
  }

  private def report(event: Event): AllureResults = {
    val results = new AllureResultsWriterStub()
    val reporter = new AllureScalatest(new AllureLifecycle(results))
    try {
      reporter(event)
    } finally {
      attach(results)
    }
    results
  }

  private def run(clazz: Class[_]): AllureResults = {
    RunUtils.runTests { _ =>
      val args = new ListBuffer[String]
      args += "-s"
      args += clazz.getCanonicalName
      args += "-C"
      args += classOf[AllureScalatest].getCanonicalName

      try {
        Runner.run(args.toArray)
      } catch {
        case _: Throwable => ()
      }
    }
  }

}
