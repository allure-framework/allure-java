Adding JUnit5-assert may lead to java.lang.OutOfMemoryError: Java heap space

Having a huge class path may lead to OOM, because AspectJ processes all the classes. [Link to documentation](https://www.eclipse.org/aspectj/doc/released/devguide/ltw.html) 

You can configure AspectJ to only process required classes
* a) package io.qameta.allure -> to process Allure classes
* b) package org.junit -> to process JUnit classes
* c) package com.company -> to process your test classes

Link to documentation https://www.eclipse.org/aspectj/doc/released/devguide/ltw-configuration.html#configuring-load-time-weaving-with-aopxml-files

In order to avoid this issue add aop.xml into `resources/META-INF/` folder. 

```<?xml version="1.0" encoding="utf-8"?>
<aspectj>
    <weaver>
        <include within="io.qameta..*" />
        <include within="org.junit..*" />
        <include within="com.company..*" />
    </weaver>
</aspectj>```
