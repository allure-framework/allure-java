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
package io.qameta.allure.spock2.compiler;

import io.qameta.allure.Allure;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ConstructorNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.GroovyClassVisitor;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.VariableScopeVisitor;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.spockframework.compiler.AstNodeCache;
import org.spockframework.compiler.AstUtil;
import org.spockframework.compiler.ErrorReporter;
import org.spockframework.compiler.SourceLookup;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author charlie (Dmitry Baev).
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class AllureStatementsLabelsToStepsTransformation implements ASTTransformation {

    @Override
    public void visit(final ASTNode[] nodes, final SourceUnit sourceUnit) {
        new Impl().visit(sourceUnit);
    }

    /**
     * Use of nested class defers linking until after groovy version check.
     */
    private static class Impl {
        private static final AstNodeCache NODE_CACHE = new AstNodeCache();

        private void visit(final SourceUnit sourceUnit) {
            final ErrorReporter errorReporter = new ErrorReporter(sourceUnit);
            final SourceLookup sourceLookup = new SourceLookup(sourceUnit);

            try {
                sourceUnit.getAST().getClasses().stream()
                        .filter(this::isSpec)
                        .forEach(clazz -> processSpec(sourceUnit, clazz, errorReporter));
            } finally {
                sourceLookup.close();
            }
        }

        private boolean isSpec(final ClassNode clazz) {
            return clazz.isDerivedFrom(NODE_CACHE.Specification);
        }

        private void processSpec(final SourceUnit sourceUnit,
                                 final ClassNode clazz,
                                 final ErrorReporter errorReporter) {
            try {
                clazz.visitContents(new MethodVisitor());
                if (!sourceUnit.getErrorCollector().hasErrors()) {
                    new VariableScopeVisitor(sourceUnit).visitClass(clazz);
                }
            } catch (Exception e) {
                errorReporter.error(
                        "Unexpected error during compilation of spec '%s'."
                        + " Please file a bug report at https://github.com/allure-framework/allure-java.",
                        e, clazz.getName()
                );
            }
        }
    }

    /**
     * Adds Allure steps for statements with labels.
     */
    private static class MethodVisitor implements GroovyClassVisitor {

        private static final ClassNode ALLURE = ClassHelper.makeWithoutCaching(Allure.class);

        @Override
        public void visitClass(final ClassNode node) {
            // do nothing
        }

        @Override
        public void visitConstructor(final ConstructorNode node) {
            // do nothing
        }

        @Override
        public void visitMethod(final MethodNode node) {
            final List<Statement> newStatements = AstUtil.getStatements(node).stream()
                    .flatMap(statement -> Stream.concat(
                            getAllureStepStatements(statement),
                            Stream.of(statement)
                    ))
                    .collect(Collectors.toList());

            node.setCode(new BlockStatement(
                    newStatements,
                    ((BlockStatement) node.getCode()).getVariableScope())
            );
        }

        @Override
        public void visitField(final FieldNode node) {
            // do nothing
        }

        @Override
        public void visitProperty(final PropertyNode node) {
            // do nothing
        }

        @SuppressWarnings("PMD.AvoidDeeplyNestedIfStmts")
        private static Stream<Statement> getAllureStepStatements(final Statement statement) {
            if (Objects.nonNull(statement.getStatementLabels())
                && !statement.getStatementLabels().isEmpty()) {
                final String labels = String.join(" ", statement.getStatementLabels());
                final StringBuilder builder = new StringBuilder(labels);

                if (statement instanceof ExpressionStatement) {
                    final ExpressionStatement es = (ExpressionStatement) statement;
                    if (es.getExpression() instanceof ConstantExpression) {
                        final Object value = ((ConstantExpression) es.getExpression()).getValue();
                        if (value instanceof String) {
                            builder.append(": ").append(value);
                        }
                    }
                }

                final String stepName = builder.toString();

                return Stream.of(new ExpressionStatement(
                        new StaticMethodCallExpression(
                                ALLURE,
                                "step",
                                new ArgumentListExpression(
                                        new ConstantExpression(stepName)
                                )
                        )
                ));
            }
            return Stream.empty();
        }
    }
}
