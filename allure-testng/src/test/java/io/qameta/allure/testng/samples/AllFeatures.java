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

import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.testng.SkipException;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

/**
 * @author ehborisov
 */
public class AllFeatures {

    @BeforeSuite
    public void beforeSuite1() {
        // throw new RuntimeException("Skip all tests"); - uncomment to skip all tests
    }

    @BeforeSuite
    public void beforeSuite2() throws IOException {
        addImgAttachment();
    }

    @BeforeGroups
    public void beforeGroups1() throws IOException {
        addImgAttachment();
    }

    @BeforeGroups
    public void beforeGroups2() throws IOException {
        addImgAttachment();
        addStringAttachment();
    }

    @BeforeTest
    public void beforeTest1() throws IOException {
        addImgAttachment();
        addStringAttachment();
    }

    @BeforeTest
    public void beforeTest2() throws IOException {
        addImgAttachment();
        addStringAttachment();
    }

    @BeforeMethod
    public void beforeMethod1() throws IOException {
        step1("in before method1");
    }

    @BeforeMethod
    public void beforeMethod2() throws IOException {
        step2("a", "in before method 2");
    }

    @Test
    public void test1() throws IOException {
        step1("pararam");
    }

    @Test
    public void skippedTest() throws IOException {
        step1("from skipped testng");
        throw new SkipException("Test was skipped");
    }

    @Step("Step 1")
    public void step1(String param) throws IOException {
        step3();
        addStringAttachment();
        addImgAttachment();
    }

    @DataProvider(name = "dataProvider")
    public Object[][] getTestData() {
        return new Object[][]{
                {"param11", "param12"},
                {"param21", "param22"}
        };
    }

    @Test(dataProvider = "dataProvider")
    public void test2(String param1, String param2) throws IOException {
        step1("pararam");
        step2(param1, param2);
    }

    @Step("Step 2")
    public void step2(String param1, String param2) throws IOException {
        addStringAttachment();
        addImgAttachment();
    }

    @Step("Step 3")
    public void step3() {

    }

    @AfterSuite
    public void afterSuite1() throws IOException {
        addImgAttachment();
        addStringAttachment();
    }

    @AfterSuite
    public void afterSuite2() throws IOException {
        step2("from after suite 2", "");
    }

    @AfterGroups
    public void afterGroups1() throws IOException {
        addImgAttachment();
        addStringAttachment();
    }

    @AfterGroups
    public void afterGroups2() throws IOException {
        addImgAttachment();
        addStringAttachment();
    }

    @AfterTest
    public void afterTest1() throws IOException {
        addImgAttachment();
        addStringAttachment();
    }

    @AfterTest
    public void afterTest2() throws IOException {
        addImgAttachment();
        addStringAttachment();
    }

    @AfterMethod
    public void afterMethod1() throws IOException {
        step1("in after method 1");
    }

    @AfterMethod
    public void afterMethod2() throws IOException {
        step2("1", "in after method 2");
    }

    @Attachment("Image attachment")
    public byte[] addImgAttachment() throws IOException {
        File img = new File(getClass().getClassLoader().getResource("totally-open-source-kitten.jpeg").getFile());
        return Files.readAllBytes(img.toPath());
    }

    @Attachment(value = "String attachment", type = "text/plain")
    public String addStringAttachment() {
        return Arrays.toString(new Throwable().getStackTrace());
    }
}
