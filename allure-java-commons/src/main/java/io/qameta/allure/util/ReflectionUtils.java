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
package io.qameta.allure.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ReflectionUtils {
    /**
     * The function gives back all the methods that users have declared in subclasses and interfaces.
     *
     * @param clazz An introspecting class.
     * @return All a user declared methods.
     */
    public static List<Method> getAllDeclaredMethods(final Class<?> clazz) {
        List<Method> methods = new ArrayList<>();

        doRecursivelyWith(
                clazz,
                ReflectionUtils::extractMethods,
                methods::add
        );

        return methods;
    }

    /**
     * The function gives back all the annotations that users have declared in subclasses and interfaces.
     *
     * @param annotatedElement An introspecting element.
     * @return All a user declared annotations.
     */
    public static List<Annotation> getAllAnnotations(final AnnotatedElement annotatedElement) {
        if (annotatedElement instanceof Class) {
            List<Annotation> annotations = new ArrayList<>();

            doRecursivelyWith(
                    (Class<?>) annotatedElement,
                    ReflectionUtils::extractAnnotations,
                    annotations::add
            );

            return annotations;
        } else {
            return Arrays.asList(annotatedElement.getAnnotations());
        }
    }

    private static void extractMethods(final Class<?> clazz, final Consumer<Method> callback) {
        Arrays.stream(clazz.getDeclaredMethods())
                .forEach(callback);

        // The default methods might have test declarations, them are needed to introspect.
        Arrays.stream(clazz.getInterfaces())
                .flatMap(ifc -> Stream.of(ifc.getMethods()))
                .filter(Method::isDefault)
                .forEach(callback);
    }

    private static void extractAnnotations(final Class<?> clazz, final Consumer<Annotation> callback) {
        Arrays.stream(clazz.getAnnotations())
                .forEach(callback);

        Arrays.stream(clazz.getInterfaces())
                .flatMap(ifc -> Stream.of(ifc.getAnnotations()))
                .forEach(callback);
    }

    private static <T> void doRecursivelyWith(
            final Class<?> clazz,
            final BiConsumer<Class<?>, Consumer<T>> introspector,
            final Consumer<T> resultCallback
    ) {
        if (clazz == Object.class) {
            return;
        }

        introspector.accept(clazz, resultCallback);

        if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
            doRecursivelyWith(clazz.getSuperclass(), introspector, resultCallback);
        } else if (clazz.isInterface()) {
            for (Class<?> superIfc : clazz.getInterfaces()) {
                doRecursivelyWith(superIfc, introspector, resultCallback);
            }
        }
    }
}
