/*
 *  Copyright 2016-2024 Qameta Software Inc
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
package io.qameta.allure.testng.samples;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Make sure this class can run without causing a ConcurrentModificationException.
 */
public class ParallelDataProviderSample {

  @DataProvider(parallel = true)
  Iterator<Integer[]> provide() {
    List<Integer[]> ret = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      ret.add(new Integer[]{i});
    }
    return ret.iterator();
  }

  @Test(dataProvider = "provide", invocationCount = 2, threadPoolSize = 2)
  public void checkCME(Integer i) {
    Assert.assertNotNull(i);
  }
}
