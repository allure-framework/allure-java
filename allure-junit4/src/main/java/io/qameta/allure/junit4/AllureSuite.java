/*
 *  Copyright 2020 Qameta Software OÜ
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
package io.qameta.allure.junit4;

import io.qameta.allure.testfilter.FileTestPlanSupplier;
import io.qameta.allure.testfilter.TestPlan;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

/**
 * @author eroshenkoam (Artem Eroshenko).
 */
public class AllureSuite extends Suite {

    private static final char UNIX_SEPARATOR = '/';
    private static final char DOT_SYMBOL = '.';

    private static final String CLASS_SUFFIX = ".class";
    private static final String FALLBACK_CLASSPATH_PROPERTY = "java.class.path";

    public AllureSuite(final Class<?> klass, final RunnerBuilder builder) throws InitializationError {
        this(klass, builder, new FileTestPlanSupplier().supply());
    }

    public AllureSuite(final Class<?> klass,
                       final RunnerBuilder builder,
                       final Optional<TestPlan> testPlan) throws InitializationError {
        super(builder, klass, findAllTestClasses());
        if (testPlan.isPresent()) {
            final Filter filter = new AllureFilter(testPlan.get());
            try {
                filter(filter);
            } catch (NoTestsRemainException e) {
                throw new InitializationError(e);
            }
        }
    }

    private static Class<?>[] findAllTestClasses() {
        final List<Path> classRoots = splitClassPath(getClasspath()).stream()
                .map(Paths::get)
                .filter(AllureSuite::isNotJar)
                .collect(Collectors.toList());
        final List<String> classFiles = new ArrayList<>();

        for (final Path classRoot : classRoots) {
            classFiles.addAll(getRelativeClassFiles(classRoot));
        }

        final List<String> classNames = classFiles.stream()
                .map(AllureSuite::classNameFromFile)
                .collect(Collectors.toList());

        final List<Class<?>> classes = classNames.stream()
                .map(AllureSuite::readClass)
                .filter(AllureSuite::isTestClass)
                .collect(Collectors.toList());

        return classes.toArray(new Class[]{});
    }

    private static List<String> getRelativeClassFiles(final Path root) {
        try {
            return Files.walk(root)
                    .filter(Files::isRegularFile)
                    .filter(Files::isReadable)
                    .filter(AllureSuite::isNotInnerClass)
                    .filter(AllureSuite::isClassFile)
                    .map(root::relativize)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private static String classNameFromFile(final String classFileName) {
        final String result = replaceFileSeparators(cutOffExtension(classFileName));
        return result.charAt(0) == '.' ? result.substring(1) : result;
    }

    private static boolean isTestClass(final Class<?> clazz) {
        if (isAbstractClass(clazz)) {
            return false;
        }
        return hasInheritanceTestMethods(clazz);
    }

    private static boolean hasInheritanceTestMethods(final Class<?> clazz) {
        Class<?> possibleClass = clazz;
        while (possibleClass != null) {
            if (hasTestMethods(possibleClass)) {
                return true;
            }
            possibleClass = possibleClass.getSuperclass();
        }
        return false;
    }

    private static boolean hasTestMethods(final Class<?> clazz) {
        return Arrays.stream(clazz.getMethods())
                .anyMatch(method -> method.isAnnotationPresent(Test.class));
    }

    private static boolean isAbstractClass(final Class<?> clazz) {
        return (clazz.getModifiers() & Modifier.ABSTRACT) != 0;
    }

    private static Class<?> readClass(final String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static boolean isNotInnerClass(final Path classFilePath) {
        return !classFilePath.getFileName().toString().contains("$");
    }

    private static boolean isClassFile(final Path classFilePath) {
        return classFilePath.getFileName().toString().endsWith(CLASS_SUFFIX);
    }

    private static boolean isNotJar(final Path classRoot) {
        final String rootName = classRoot.getFileName().toString();
        return !rootName.endsWith(".jar") && !rootName.endsWith(".JAR");
    }

    private static String getClasspath() {
        return System.getProperty(FALLBACK_CLASSPATH_PROPERTY);
    }

    private static List<String> splitClassPath(final String classPath) {
        final String separator = System.getProperty("path.separator");
        return Arrays.asList(classPath.split(separator));
    }

    private static String replaceFileSeparators(final String s) {
        String result = s.replace(File.separatorChar, DOT_SYMBOL);
        if (File.separatorChar != UNIX_SEPARATOR) {
            result = result.replace(UNIX_SEPARATOR, DOT_SYMBOL);
        }
        return result;
    }

    private static String cutOffExtension(final String classFileName) {
        return classFileName.substring(0, classFileName.length() - CLASS_SUFFIX.length());
    }

}
