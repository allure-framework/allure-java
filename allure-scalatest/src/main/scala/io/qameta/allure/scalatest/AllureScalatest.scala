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

import java.lang.annotation.Annotation
import java.util.{Objects, UUID}
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock

import io.qameta.allure._
import io.qameta.allure.model.{Status, StatusDetails, TestResult}
import io.qameta.allure.util.ResultsUtils._
import org.scalatest.Reporter
import org.scalatest.events._
import org.scalatest.exceptions.TestFailedException

import scala.jdk.CollectionConverters._
import scala.collection.mutable

/**
  * @author charlie (Dmitry Baev).
  */
trait AllureScalatestContext {
  AllureScalatestContextHolder.populate()
}

object AllureScalatestContextHolder {
  private val populateTimeout = TimeUnit.SECONDS.toMillis(3)
  private val lock = new ReentrantReadWriteLock()
  private val threads: mutable.HashMap[String, String] = mutable.HashMap[String, String]()

  def populate(): Unit = {
    val threadName = Thread.currentThread().getName
    var maybeUuid = get(threadName)
    val current = System.currentTimeMillis()
    while (maybeUuid.isEmpty && System.currentTimeMillis - current < populateTimeout) {
      Thread.sleep(100)
      maybeUuid = get(threadName)
    }
    maybeUuid.fold {} { uuid => Allure.getLifecycle.setCurrentTestCase(uuid) }
  }

  private[scalatest] def add(threadId: String, uuid: String): Unit = {
    lock.writeLock().lock()
    try {
      threads += threadId -> uuid
    } finally {
      lock.writeLock().unlock()
    }
  }

  private[scalatest] def get(threadId: String): Option[String] = {
    lock.readLock().lock()
    try {
      threads.get(threadId)
    } finally {
      lock.readLock().unlock()
    }
  }

  private[scalatest] def remove(threadId: String): Unit = {
    lock.writeLock().lock()
    try {
      threads -= threadId
    } finally {
      lock.writeLock().unlock()
    }
  }
}

class AllureScalatest(val lifecycle: AllureLifecycle) extends Reporter {

  private val lock = new ReentrantReadWriteLock()
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
    case event: TestIgnored => ignoreTestCase(event)
    case _ => ()
  }

  def startSuite(event: SuiteStarting): Unit = {
    setSuiteLocation(event.suiteId, event.location)
  }

  def completeSuite(event: SuiteCompleted): Unit = {
    removeSuiteLocation(event.suiteId)
  }

  def abortSuite(event: SuiteAborted): Unit = {
    removeSuiteLocation(event.suiteId)
  }

  def startTestCase(event: TestStarting): Unit = {
    startTest(
      event.suiteId,
      event.suiteName,
      event.suiteClassName,
      event.location,
      event.testName,
      Some(event.threadName)
    )
  }

  def failTestCase(event: TestFailed): Unit = {
    val throwable = event.throwable.getOrElse(new RuntimeException(event.message))
    val status = throwable match {
      case _: TestFailedException => Status.FAILED
      case _ => Status.BROKEN
    }
    val statusDetails = getStatusDetails(throwable)
      .orElse(new StatusDetails().setMessage(event.message))

    stopTest(
      Some(status),
      Some(statusDetails),
      Some(event.threadName)
    )
  }

  def passTestCase(event: TestSucceeded): Unit = {
    stopTest(
      Some(Status.PASSED),
      None,
      Some(event.threadName)
    )
  }

  def cancelTestCase(event: TestCanceled): Unit = {
    stopTest(
      Some(Status.SKIPPED),
      Some(new StatusDetails().setMessage(event.message)),
      Some(event.threadName)
    )
  }

  def ignoreTestCase(event: TestIgnored): Unit = {
    startTest(
      event.suiteId,
      event.suiteName,
      event.suiteClassName,
      event.location,
      event.testName,
      Some(event.threadName)
    )
    stopTest(
      None,
      Some(new StatusDetails().setMessage("Test ignored")),
      Some(event.threadName)
    )
  }

  private def startTest(suiteId: String,
                        suiteName: String,
                        suiteClassName: Option[String],
                        location: Option[Location],
                        testName: String,
                        threadId: Option[String]): Unit = {
    val uuid = UUID.randomUUID().toString
    var labels = mutable.ListBuffer(
      createSuiteLabel(suiteName),
      createLabel(THREAD_LABEL_NAME, getScalaTestThreadName(threadId)),
      createHostLabel(),
      createLanguageLabel("scala"),
      createFrameworkLabel("scalatest")
    )
    labels ++= getProvidedLabels.asScala

    var links = mutable.ListBuffer[io.qameta.allure.model.Link]()

    val result = new TestResult()
      .setFullName(suiteId + " " + testName)
      .setName(testName)
      .setUuid(uuid)
      .setTestCaseId(md5(suiteId + testName))
      .setHistoryId(md5(suiteId + testName))

    val testAnnotations = getAnnotations(location)
    val suiteAnnotations = getAnnotations(getSuiteLocation(suiteId))

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

    suiteClassName.map(className => createTestClassLabel(className))
      .fold {} { value => labels += value }

    result.setLabels(labels.asJava)

    lifecycle.scheduleTestCase(result)
    lifecycle.startTestCase(uuid)

    //this should be called after test case scheduled
    threadId.fold {} { thread => AllureScalatestContextHolder.add(thread, uuid) }
  }

  private def stopTest(status: Option[Status],
                       statusDetails: Option[StatusDetails],
                       threadName: Option[String]): Unit = {
    threadName.fold {} {
      thread => {
        AllureScalatestContextHolder.get(thread).fold {} {
          uuid => {
            lifecycle.updateTestCase(uuid, (result: TestResult) => {
              status.fold {} { st => result.setStatus(st) }
              statusDetails.fold {} { details => result.setStatusDetails(details) }
            }: Unit)
            lifecycle.stopTestCase(uuid)
            lifecycle.writeTestCase(uuid)
          }
            AllureScalatestContextHolder.remove(thread)
        }
      }
    }
  }

  private def getAnnotations(location: Option[Location]): List[Annotation] = location match {
    case Some(TopOfClass(className)) => Class.forName(className).getAnnotations.toList
    case Some(TopOfMethod(className, methodName)) => Class.forName(className).getMethod(methodName).getDeclaredAnnotations.toList
    case _ => List()
  }

  private def setSuiteLocation(suiteId: String, location: Option[Location]): Unit = {
    location.fold {} { l =>
      lock.writeLock().lock()
      try {
        suites += suiteId -> l
      } finally {
        lock.writeLock().unlock()
      }
    }
  }

  private def getSuiteLocation(suiteId: String): Option[Location] = {
    lock.readLock().lock()
    try {
      suites.get(suiteId)
    } finally {
      lock.readLock().unlock()
    }
  }

  private def removeSuiteLocation(suiteId: String): Unit = {
    lock.writeLock().lock()
    try {
      suites -= suiteId
    } finally {
      lock.writeLock().unlock()
    }
  }

  private def getScalaTestThreadName(threadId: Option[String]): String = {
    val fromProperty = System.getProperty(ALLURE_THREAD_NAME_SYSPROP)
    val fromEnv = System.getenv(ALLURE_THREAD_NAME_ENV)
    val realThreadName = threadId.getOrElse {getThreadName}
    Seq(fromProperty, fromEnv).find(el => Objects.nonNull(el)).getOrElse {realThreadName}
  }

}
