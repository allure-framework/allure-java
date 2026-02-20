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
package io.qameta.allure.description;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.qameta.allure.description.ClassNames.DESCRIPTION_ANNOTATION;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
@SupportedAnnotationTypes(DESCRIPTION_ANNOTATION)
public class JavaDocDescriptionsProcessor extends AbstractProcessor {

    private static final String ALLURE_DESCRIPTIONS_FOLDER = "META-INF/allureDescriptions/";

    private Filer filer;
    private Elements elementUtils;
    private Messager messager;

    @Override
    @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
    public synchronized void init(final ProcessingEnvironment env) {
        super.init(env);
        filer = env.getFiler();
        elementUtils = env.getElementUtils();
        messager = env.getMessager();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment env) {
        final TypeElement typeElement = elementUtils.getTypeElement(DESCRIPTION_ANNOTATION);
        final Set<? extends Element> elements = env.getElementsAnnotatedWith(typeElement);
        final Set<ExecutableElement> methods = ElementFilter.methodsIn(elements);
        methods.forEach(method -> {
            final String rawDocs = elementUtils.getDocComment(method);

            if (rawDocs == null) {
                return;
            }

            final String docs = rawDocs.trim();
            if (docs.isEmpty()) {
                return;
            }

            final String name = method.getSimpleName().toString();
            final List<String> typeParams = method.getParameters().stream()
                    .map(this::methodParameterTypeMapper)
                    .collect(Collectors.toList());

            final String hash = generateMethodSignatureHash(
                    method.getEnclosingElement().toString(), name, typeParams
            );
            try {
                final FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT, "",
                        ALLURE_DESCRIPTIONS_FOLDER + hash);
                try (Writer writer = file.openWriter()) {
                    writer.write(docs);
                }
            } catch (IOException e) {
                messager.printMessage(Diagnostic.Kind.WARNING,
                        "Unable to create resource from docs comment of method " + name + typeParams);
            }
        });

        return true;
    }

    private String methodParameterTypeMapper(final VariableElement parameter) {
        final Element typeElement = processingEnv.getTypeUtils().asElement(parameter.asType());
        return typeElement != null ? typeElement.toString() : parameter.asType().toString();
    }

    private static String generateMethodSignatureHash(final String className,
                                                      final String methodName,
                                                      final List<String> parameterTypes) {
        final MessageDigest md = getMd5Digest();
        md.update(className.getBytes(StandardCharsets.UTF_8));
        md.update(methodName.getBytes(StandardCharsets.UTF_8));
        parameterTypes.stream()
                .map(string -> string.getBytes(StandardCharsets.UTF_8))
                .forEach(md::update);
        final byte[] bytes = md.digest();
        return bytesToHex(bytes);
    }

    private static MessageDigest getMd5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Can not find hashing algorithm", e);
        }
    }

    private static String bytesToHex(final byte[] bytes) {
        return new BigInteger(1, bytes).toString(16);
    }
}
