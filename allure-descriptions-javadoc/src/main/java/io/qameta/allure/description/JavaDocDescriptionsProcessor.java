/*
 *  Copyright 2019 Qameta Software OÜ
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

import io.qameta.allure.Description;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.qameta.allure.util.ResultsUtils.generateMethodSignatureHash;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
@SupportedAnnotationTypes("io.qameta.allure.Description")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class JavaDocDescriptionsProcessor extends AbstractProcessor {

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
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment env) {
        final Set<? extends Element> elements = env.getElementsAnnotatedWith(Description.class);
        elements.forEach(el -> {
            if (!el.getAnnotation(Description.class).useJavaDoc()) {
                return;
            }
            final String docs = elementUtils.getDocComment(el);
            final List<String> typeParams = ((ExecutableElement) el).getParameters().stream()
                    .map(param -> param.asType().toString()).collect(Collectors.toList());
            final String name = el.getSimpleName().toString();

            final String hash = generateMethodSignatureHash(el.getEnclosingElement().toString(), name, typeParams);
            try {
                final FileObject file = filer.createResource(StandardLocation.CLASS_OUTPUT,
                        "allureDescriptions", hash);
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
}
