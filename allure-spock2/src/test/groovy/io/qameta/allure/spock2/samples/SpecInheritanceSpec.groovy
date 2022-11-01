/*
 *  Copyright 2022 Qameta Software OÜ
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
package io.qameta.allure.spock2.samples

import spock.lang.Specification

abstract class BaseSpec extends Specification {
  def setupSpec() { println 'base setupSpec()' }
  def cleanupSpec() { println 'base cleanupSpec()' }

  def setup() { println 'base setup()' }
  def cleanup() { println 'base cleanup()' }

  def baseSpecMethod() { setup: println 'base spec method' }
}

class DerivedSpec extends BaseSpec {
  def setupSpec() { println 'derived setupSpec()' }
  def cleanupSpec() { println 'derived cleanupSpec()' }

  def setup() { println 'derived setup()' }
  def cleanup() { println 'derived cleanup()' }

  def derivedSpecMethod() { setup: println 'derived spec method' }
}
