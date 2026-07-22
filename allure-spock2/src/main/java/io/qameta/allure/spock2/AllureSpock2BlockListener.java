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
package io.qameta.allure.spock2;

import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.StepResult;
import org.spockframework.runtime.extension.IBlockListener;
import org.spockframework.runtime.extension.builtin.UnrollIterationNameProvider;
import org.spockframework.runtime.model.BlockInfo;
import org.spockframework.runtime.model.BlockKind;
import org.spockframework.runtime.model.ErrorInfo;
import org.spockframework.runtime.model.IterationInfo;
import spock.lang.Specification;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.qameta.allure.util.ResultsUtils.getStatus;
import static io.qameta.allure.util.ResultsUtils.getStatusDetails;

/**
 * Reports executable Spock blocks as Allure steps.
 */
final class AllureSpock2BlockListener implements IBlockListener {

    private static final String BLOCK = "block";

    private final ThreadLocal<Map<BlockInfo, BlockStep>> blockSteps = new ThreadLocal<>();

    private final ThreadLocal<BlockStep> activeBlockStep = new ThreadLocal<>();

    private final ThreadLocal<AllureExternalKey> testKeys = new ThreadLocal<>();

    private final AllureLifecycle lifecycle;

    AllureSpock2BlockListener(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    void beforeIteration(final AllureExternalKey testKey) {
        blockSteps.set(new IdentityHashMap<>());
        activeBlockStep.remove();
        testKeys.set(testKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <S extends Specification> void blockEntered(final S specificationInstance, final BlockInfo blockInfo) {
        final Map<BlockInfo, BlockStep> steps = blockSteps.get();
        if (Objects.isNull(testKeys.get()) || Objects.isNull(steps)) {
            return;
        }

        stopActiveBlockStep();

        final IterationInfo iteration = specificationInstance.getSpecificationContext().getCurrentIteration();
        if (Objects.isNull(iteration)) {
            return;
        }

        final StepResult result = new StepResult()
                .setName(getBlockName(blockInfo, iteration));
        final BlockStep blockStep = new BlockStep(blockStepKey(), result);

        lifecycle.startStep(blockStep.key, result);
        steps.put(blockInfo, blockStep);
        activeBlockStep.set(blockStep);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <S extends Specification> void blockExited(final S specificationInstance, final BlockInfo blockInfo) {
        final Map<BlockInfo, BlockStep> steps = blockSteps.get();
        if (Objects.isNull(steps)) {
            return;
        }

        final BlockStep blockStep = steps.get(blockInfo);
        if (Objects.isNull(blockStep)) {
            return;
        }

        blockStep.result.setStatus(Status.PASSED);
        if (Objects.equals(blockStep, activeBlockStep.get())) {
            lifecycle.updateStep(blockStep.key, step -> step.setStatus(Status.PASSED));
            stopActiveBlockStep();
        }
    }

    void error(final ErrorInfo error) {
        if (Objects.isNull(error.getErrorContext()) || Objects.isNull(error.getErrorContext().getBlock())) {
            return;
        }

        final Map<BlockInfo, BlockStep> steps = blockSteps.get();
        if (Objects.isNull(steps)) {
            return;
        }

        final BlockStep blockStep = steps.get(error.getErrorContext().getBlock());
        if (Objects.isNull(blockStep)) {
            return;
        }

        final Throwable exception = error.getException();
        final Status status = getStatus(exception).orElse(Status.BROKEN);
        final StatusDetails details = getStatusDetails(exception).orElse(null);
        blockStep.result
                .setStatus(status)
                .setStatusDetails(details);

        if (Objects.equals(blockStep, activeBlockStep.get())) {
            lifecycle.updateStep(
                    blockStep.key,
                    step -> step
                            .setStatus(status)
                            .setStatusDetails(details)
            );
            stopActiveBlockStep();
        }
    }

    void afterIteration() {
        try {
            stopActiveBlockStep();
        } finally {
            blockSteps.remove();
            activeBlockStep.remove();
            testKeys.remove();
        }
    }

    private String getBlockName(final BlockInfo blockInfo, final IterationInfo iteration) {
        final String kind = BlockKind.SETUP.equals(blockInfo.getKind())
                ? "given"
                : Optional.ofNullable(blockInfo.getKind())
                        .map(Enum::name)
                        .map(name -> name.toLowerCase(Locale.ENGLISH))
                        .orElse(BLOCK);
        final List<String> texts = blockInfo.getTexts();
        if (Objects.isNull(texts) || texts.isEmpty()) {
            return kind;
        }

        final String description = texts.stream()
                .filter(Objects::nonNull)
                .map(text -> resolveBlockText(iteration, text))
                .collect(Collectors.joining(", "));
        return description.isEmpty() ? kind : kind + ": " + description;
    }

    private String resolveBlockText(final IterationInfo iteration, final String text) {
        try {
            return new UnrollIterationNameProvider(iteration.getFeature(), text, false).getName(iteration);
        } catch (RuntimeException ignored) {
            return text;
        }
    }

    private void stopActiveBlockStep() {
        final BlockStep blockStep = activeBlockStep.get();
        if (Objects.isNull(blockStep)) {
            return;
        }

        final boolean isCurrent = lifecycle.getCurrentExecutableKey()
                .filter(blockStep.key::equals)
                .isPresent();
        if (isCurrent) {
            lifecycle.stopStep();
        } else {
            lifecycle.stopStep(blockStep.key);
            Optional.ofNullable(testKeys.get()).ifPresent(lifecycle::setCurrent);
        }
        activeBlockStep.remove();
    }

    private AllureExternalKey blockStepKey() {
        return AllureExternalKey.of(AllureSpock2.class, BLOCK, UUID.randomUUID().toString());
    }

    private static final class BlockStep {

        private final AllureExternalKey key;

        private final StepResult result;

        private BlockStep(final AllureExternalKey key, final StepResult result) {
            this.key = key;
            this.result = result;
        }
    }
}
