Adding JUnit5-assert leads to java.lang.OutOfMemoryError: Java heap space

In order to get rid of this issue add aop.xml into `resources/META-INF/` folder. 

`com.company..` is the package with your tests.

```<?xml version="1.0" encoding="utf-8"?>
<aspectj>
    <weaver>
        <include within="io.qameta..*" />
        <include within="org.junit..*" />
        <include within="com.company..*" />
    </weaver>
</aspectj>```
