/*
 *  Copyright 2019 Qameta Software OÜ
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
import io.qameta.allure.test.{AllureResults, AllureResultsWriterStub}
import io.qameta.allure.{Allure, AllureLifecycle}
import org.junit.jupiter.api.Test
import org.scalatest.matchers.should.Matchers._
import org.scalatest.tools.Runner

import scala.collection.JavaConverters.asScala
import scala.collection.mutable.ListBuffer

/**
  * @author charlie (Dmitry Baev).
  */
class AllureScalatestTest {

  @Test
  def shouldSetName(): Unit = {
    val results = run(classOf[SimpleSpec])
    asScala(results.getTestResults)
      .map(item => item.getName) should contain("test should be passed")
  }

  @Test
  def shouldSetStart(): Unit = {
    val results = run(classOf[SimpleSpec])

    val starts = asScala(results.getTestResults)
      .map(item => item.getStart).toList

    every(starts) should not be null
  }

  @Test
  def shouldSetStop(): Unit = {
    val results = run(classOf[SimpleSpec])

    val stops = asScala(results.getTestResults)
      .map(item => item.getStop)

    every(stops) should not be null
  }

  @Test
  def shouldSetStage(): Unit = {
    val results = run(classOf[SimpleSpec])

    val stages = asScala(results.getTestResults)
      .map(item => item.getStage)

    every(stages) shouldBe FINISHED
  }

  @Test
  def shouldSetStatus(): Unit = {
    val results = run(classOf[SimpleSpec])

    val statuses = asScala(results.getTestResults)
      .map(item => item.getStatus)

    every(statuses) shouldBe Status.PASSED
  }

  @Test
  def shouldSetFailedStatus(): Unit = {
    val results = run(classOf[FailedSpec])

    results.getTestResults should have length 1

    val statuses = asScala(results.getTestResults)
      .map(item => item.getStatus)

    every(statuses) shouldBe Status.FAILED
  }

  @Test
  def shouldSetBrokenStatus(): Unit = {
    val results = run(classOf[BrokenSpec])

    results.getTestResults should have length 1

    val statuses = asScala(results.getTestResults)
      .map(item => item.getStatus).toList

    every(statuses) should be(Status.BROKEN)
  }

  @Test
  def shouldSetSkippedStatus(): Unit = {
    val results = run(classOf[CancelledSpec])

    results.getTestResults should have length 1

    val statuses = asScala(results.getTestResults)
      .map(item => item.getStatus).toList

    every(statuses) should be(Status.SKIPPED)
  }

  @Test
  def shouldProcessSuiteAnnotations(): Unit = {
    val results = run(classOf[AnnotationsOnClassSpec])

    results.getTestResults should have length 1

    val labels = asScala(results.getTestResults)
      .flatMap(item => asScala(item.getLabels))
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

    val labels = asScala(results.getTestResults)
      .flatMap(item => asScala(item.getLabels))
      .map(label => (label.getName, label.getValue))
      .toList

    labels should contain(("severity", "blocker"))
  }

  @Test
  def shouldProcessIgnoredTests(): Unit = {
    val results = run(classOf[IgnoredSpec])

    results.getTestResults should have length 1

    asScala(results.getTestResults)
      .map(item => item.getName) should contain("test should be ignored")

    val statuses = asScala(results.getTestResults)
      .map(item => item.getStatus).toList

    every(statuses) should be(null)

    val stages = asScala(results.getTestResults)
      .map(item => item.getStage).toList

    every(stages) should be(Stage.FINISHED)
  }

  @Test
  def shouldSupportJavaApi(): Unit = {
    val results = run(classOf[AllureApiSpec])
    val steps = asScala(results.getTestResults)
      .flatMap(item => asScala(item.getSteps))

    steps
      .map(step => step.getName) should contain inOrder("first", "second", "third")

    steps.filter(step => step.getName == "second")
      .flatMap(step => asScala(step.getSteps))
      .map(step => step.getName) should contain inOrder("child1", "child2", "child3")

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
