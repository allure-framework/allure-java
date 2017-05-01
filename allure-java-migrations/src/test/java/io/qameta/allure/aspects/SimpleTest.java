package io.qameta.allure.aspects;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * eroshenkoam
 * 01.05.17
 */
public class SimpleTest {

    @Test
    public void simpleTest() {
        Method method = MethodUtils.getMatchingMethod(TestClass.class, "test", String.class);
        Arrays.asList(method.getParameters()).forEach(System.out::println);
    }

    public static class TestClass{

        private void test(String parameter) {

        }
    }
}
