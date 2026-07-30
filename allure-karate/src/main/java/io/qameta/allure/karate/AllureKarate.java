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
package io.qameta.allure.karate;

import io.karatelabs.common.Json;
import io.karatelabs.core.HttpRunEvent;
import io.karatelabs.core.RunEvent;
import io.karatelabs.core.RunListener;
import io.karatelabs.core.ScenarioResult;
import io.karatelabs.core.ScenarioRunEvent;
import io.karatelabs.core.ScenarioRuntime;
import io.karatelabs.core.StepResult;
import io.karatelabs.core.StepRunEvent;
import io.karatelabs.gherkin.Feature;
import io.karatelabs.gherkin.Scenario;
import io.karatelabs.gherkin.Step;
import io.karatelabs.gherkin.Tag;
import io.karatelabs.http.HttpRequest;
import io.karatelabs.http.HttpResponse;
import io.karatelabs.output.LogMask;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.AttachmentOptions;
import io.qameta.allure.http.HttpExchange;
import io.qameta.allure.http.HttpExchangeBody;
import io.qameta.allure.http.HttpExchangeNameValue;
import io.qameta.allure.http.HttpExchangeRequest;
import io.qameta.allure.http.HttpExchangeResponse;
import io.qameta.allure.http.HttpExchangeSerializer;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.Link;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.util.ResultsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.qameta.allure.util.ResultsUtils.createLabel;
import static io.qameta.allure.util.ResultsUtils.createLink;
import static io.qameta.allure.util.ResultsUtils.createParameter;
import static io.qameta.allure.util.ResultsUtils.createTitlePath;
import static io.qameta.allure.util.ResultsUtils.createTitlePathFromSourcePath;
import static io.qameta.allure.util.ResultsUtils.md5;

/**
 * Reports Karate runtime events to Allure.
 *
 * <p>Register this listener with Karate so features, scenarios, steps, and attachments are converted into Allure
 * results. The listener uses the Allure lifecycle to write standard result files.</p>
 */
@SuppressWarnings({"MultipleStringLiterals", "PMD.GodClass", "PMD.TooManyMethods"})
public class AllureKarate implements RunListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureKarate.class);

    private static final String BUILD_RESOURCES = "build/resources/";
    private static final String HTTP_EXCHANGE_ATTACHMENT = "HTTP exchange";
    private static final String IMAGE_DIFF_CONTENT_TYPE = "application/vnd.allure.image.diff";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String URI_LIST_CONTENT_TYPE = "text/uri-list";
    private static final String BINARY_CONTENT_TYPE = "application/octet-stream";

    private final AllureLifecycle lifecycle;

    private final Map<ScenarioRuntime, ScenarioContext> scenarioContexts = new ConcurrentHashMap<>();
    private final Map<ScenarioRuntime, AllureExternalKey> activeStepKeys = new ConcurrentHashMap<>();
    private final Set<AllureExternalKey> redactedStepKeys = ConcurrentHashMap.newKeySet();
    private final Set<ScenarioRuntime> redactedScenarioFailures = ConcurrentHashMap.newKeySet();

    /**
     * Creates an Allure karate with default configuration.
     */
    public AllureKarate() {
        this(Allure.getLifecycle());
    }

    /**
     * Creates an Allure karate with the supplied values.
     *
     * @param lifecycle the Allure lifecycle to use
     */
    public AllureKarate(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onEvent(final RunEvent event) {
        switch (event.getType()) {
            case SCENARIO_ENTER:
                return beforeScenario(((ScenarioRunEvent) event).source());
            case SCENARIO_EXIT:
                afterScenario((ScenarioRunEvent) event);
                return true;
            case STEP_ENTER:
                return beforeStep((StepRunEvent) event);
            case STEP_EXIT:
                afterStep((StepRunEvent) event);
                return true;
            case HTTP_EXIT:
                afterHttp((HttpRunEvent) event);
                return true;
            default:
                return true;
        }
    }

    private boolean beforeScenario(final ScenarioRuntime sr) {
        if (sr.getFeatureRuntime().isCalled()) {
            return beforeCalledScenario(sr);
        }

        final Scenario scenario = sr.getScenario();
        final Feature feature = scenario.getFeature();
        final String featureName = feature.getName();
        final String featureNameQualified = getFeatureNameQualified(feature);

        final String uuid = UUID.randomUUID().toString();

        final String nameOrLine = getName(scenario, String.valueOf(scenario.getLine()));
        final String testCaseId = md5(String.format("%s:%s", featureNameQualified, nameOrLine));
        final String fullName = String.format("%s:%d", featureNameQualified, scenario.getLine());
        final List<String> titlePath = createTitlePathFromSourcePath(featureNameQualified);
        titlePath.addAll(createTitlePath(featureName));
        final TestResult result = new TestResult()
                .setUuid(uuid)
                .setFullName(fullName)
                .setName(getName(scenario, fullName))
                .setDescription(sr.isReportDisabled() ? null : getDescription(scenario))
                .setTestCaseId(testCaseId)
                .setTitlePath(titlePath);

        final List<String> labels = getTagTexts(scenario);
        result.setLabels(getLabels(labels));

        final List<Link> links = getLinks(labels);
        if (!links.isEmpty()) {
            result.setLinks(links);
        }

        final AllureExternalKey testKey = testKey(uuid);
        lifecycle.scheduleTest(testKey, result);
        lifecycle.addDefaultLabels(testKey, List.of(ResultsUtils.createFeatureLabel(featureName)));
        lifecycle.startTest(testKey);
        scenarioContexts.put(
                sr,
                new ScenarioContext(uuid, testKey, null, null, null, true, sr.isReportDisabled())
        );
        return true;
    }

    private boolean beforeCalledScenario(final ScenarioRuntime sr) {
        final ScenarioRuntime caller = sr.getFeatureRuntime().getCallerScenario();
        if (Objects.isNull(caller)) {
            return true;
        }
        final ScenarioContext callerContext = scenarioContexts.get(caller);
        if (Objects.isNull(callerContext)) {
            return true;
        }

        final AllureExternalKey parentStepKey = activeStepKeys.get(caller);
        final AllureExternalKey parentKey = Objects.requireNonNullElse(parentStepKey, callerContext.ownerKey());
        if (sr.isReportDisabled()) {
            scenarioContexts.put(
                    sr,
                    new ScenarioContext(
                            callerContext.testUuid(),
                            parentKey,
                            null,
                            parentStepKey,
                            caller,
                            false,
                            true
                    )
            );
            return true;
        }

        final AllureExternalKey scenarioKey = AllureExternalKey.random(AllureKarate.class);
        lifecycle.startStep(
                parentKey,
                scenarioKey,
                new io.qameta.allure.model.StepResult().setName(getCalledScenarioName(sr.getScenario()))
        );
        scenarioContexts.put(
                sr,
                new ScenarioContext(
                        callerContext.testUuid(),
                        scenarioKey,
                        scenarioKey,
                        parentStepKey,
                        caller,
                        false,
                        false
                )
        );
        return true;
    }

    private static AllureExternalKey testKey(final String scenarioUuid) {
        return AllureExternalKey.of(AllureKarate.class, "test", scenarioUuid);
    }

    private static String getCalledScenarioName(final Scenario scenario) {
        final Feature feature = scenario.getFeature();
        final String featureName = Objects.isNull(feature.getName()) || feature.getName().isBlank()
                ? getFeatureNameQualified(feature)
                : feature.getName().trim();
        return featureName + ": " + getName(scenario, String.valueOf(scenario.getLine()));
    }

    private static String getName(final Scenario scenario, final String defaultValue) {
        if (Objects.isNull(scenario.getName()) || scenario.getName().trim().startsWith("#")) {
            return defaultValue;
        }
        final boolean blank = scenario.getName().chars()
                .allMatch(Character::isWhitespace);
        return blank ? defaultValue : scenario.getName().trim();
    }

    private static String getDescription(final Scenario scenario) {
        final String description = scenario.getDescription();
        if (Objects.isNull(description)) {
            return "";
        }
        return description.lines()
                .filter(line -> !line.trim().startsWith("#"))
                .collect(Collectors.joining("\n"))
                .trim();
    }

    private static String getFeatureNameQualified(final Feature feature) {
        final String path = feature.getResource().getRelativePath().replace('\\', '/');
        final int resourcesIndex = path.indexOf(BUILD_RESOURCES);
        if (resourcesIndex < 0) {
            return path;
        }
        final int sourceSetIndex = path.indexOf('/', resourcesIndex + BUILD_RESOURCES.length());
        return sourceSetIndex < 0 ? path : path.substring(sourceSetIndex + 1);
    }

    private void afterScenario(final ScenarioRunEvent event) {
        final ScenarioRuntime sr = event.source();
        final ScenarioContext context = scenarioContexts.remove(sr);
        activeStepKeys.remove(sr);
        if (Objects.isNull(context)) {
            return;
        }
        final Optional<ScenarioResult> maybeResult = Optional.ofNullable(event.result());

        final boolean failed = maybeResult.map(ScenarioResult::isFailed).orElse(true);
        final Throwable error = maybeResult.map(ScenarioResult::getError).orElse(null);
        final boolean redactFailure = context.reportDisabled() || redactedScenarioFailures.remove(sr);
        final Status status = getStatus(failed, error);
        final StatusDetails statusDetails = getStatusDetails(failed, error, redactFailure);

        if (!context.topLevel()) {
            finishCalledScenario(context, failed, redactFailure, status, statusDetails);
            return;
        }

        final List<Parameter> list = new ArrayList<>();
        if (!context.reportDisabled()
                && event.result() != null
                && event.result().getScenario().getExampleIndex() > -1) {
            final Map<String, Object> data = event.result().getScenario().getExampleData();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                list.add(createParameter(entry.getKey(), entry.getValue()));
            }
        }

        final AllureExternalKey testKey = testKey(context.testUuid());
        lifecycle.updateTest(testKey, tr -> {
            tr.setStatus(status);
            tr.setStatusDetails(statusDetails);
            tr.getParameters().addAll(list);
        });

        lifecycle.stopTest(testKey);
        lifecycle.writeTest(testKey);
    }

    private void finishCalledScenario(final ScenarioContext context,
                                      final boolean failed,
                                      final boolean redactFailure,
                                      final Status status,
                                      final StatusDetails statusDetails) {
        if (failed && redactFailure) {
            if (Objects.nonNull(context.parentStepKey())) {
                redactedStepKeys.add(context.parentStepKey());
            }
            if (Objects.nonNull(context.callerRuntime())) {
                redactedScenarioFailures.add(context.callerRuntime());
            }
        }
        if (Objects.isNull(context.scenarioKey())) {
            return;
        }
        lifecycle.updateStep(context.scenarioKey(), step -> {
            step.setStatus(status);
            step.setStatusDetails(statusDetails);
        });
        lifecycle.stopStep(context.scenarioKey());
    }

    private boolean beforeStep(final StepRunEvent event) {
        final ScenarioRuntime scenarioRuntime = event.scenarioRuntime();
        final ScenarioContext context = scenarioContexts.get(scenarioRuntime);
        if (Objects.isNull(context) || context.reportDisabled()) {
            return true;
        }

        final Step step = event.step();
        final AllureExternalKey stepKey = AllureExternalKey.random(AllureKarate.class);
        final io.qameta.allure.model.StepResult stepResult = new io.qameta.allure.model.StepResult()
                .setName(getStepName(step));

        lifecycle.startStep(context.ownerKey(), stepKey, stepResult);
        activeStepKeys.put(scenarioRuntime, stepKey);

        return true;
    }

    private void afterStep(final StepRunEvent event) {
        final StepResult result = event.result();
        final ScenarioRuntime scenarioRuntime = event.scenarioRuntime();
        final ScenarioContext context = scenarioContexts.get(scenarioRuntime);
        final AllureExternalKey stepKey = activeStepKeys.remove(scenarioRuntime);
        if (Objects.isNull(context) || context.reportDisabled() || Objects.isNull(stepKey)) {
            return;
        }

        final boolean redactFailure = redactedStepKeys.remove(stepKey);
        final Status status = getStatus(result.isFailed(), result.getError());
        final StatusDetails statusDetails = getStatusDetails(result.isFailed(), result.getError(), redactFailure);

        lifecycle.updateStep(stepKey, s -> {
            s.setStatus(status);
            s.setStatusDetails(statusDetails);
        });

        if (Objects.nonNull(result.getEmbeds())) {
            result.getEmbeds().forEach(embed -> addEmbed(stepKey, embed));
        }

        lifecycle.stopStep(stepKey);

    }

    private void afterHttp(final HttpRunEvent event) {
        final ScenarioContext context = scenarioContexts.get(event.scenarioRuntime());
        final AllureExternalKey stepKey = activeStepKeys.get(event.scenarioRuntime());
        final HttpRequest request = event.request();
        if (Objects.isNull(context)
                || context.reportDisabled()
                || Objects.isNull(stepKey)
                || Objects.isNull(request)) {
            return;
        }

        final LogMask mask = getActiveMask(event.scenarioRuntime(), request.getUrlAndPath());
        final HttpResponse response = event.response();
        final HttpExchange.Builder exchange = HttpExchange.builder(toHttpExchangeRequest(request, mask))
                .setStart(Objects.isNull(response) ? null : response.getStartTime())
                .setStop(event.getTimeStamp());

        if (Objects.nonNull(response)) {
            exchange.setResponse(toHttpExchangeResponse(response, mask));
        }

        try {
            lifecycle.addAttachment(
                    stepKey,
                    HTTP_EXCHANGE_ATTACHMENT,
                    HttpExchange.CONTENT_TYPE,
                    new ByteArrayInputStream(HttpExchangeSerializer.toJsonBytes(exchange.build())),
                    AttachmentOptions.empty()
            );
        } catch (RuntimeException e) {
            LOGGER.warn("could not save HTTP exchange", e);
        }
    }

    private static LogMask getActiveMask(final ScenarioRuntime scenarioRuntime, final String uri) {
        final LogMask mask = scenarioRuntime.getConfig().getCompiledMask();
        return Objects.nonNull(mask) && mask.enabledForUri(uri) ? mask : null;
    }

    private static HttpExchangeRequest toHttpExchangeRequest(final HttpRequest request, final LogMask mask) {
        final HttpExchangeRequest.Builder builder = HttpExchangeRequest
                .builder(request.getMethod(), request.getUrlAndPath())
                .addHeaders(toNameValues(request.getHeaders(), mask))
                .setBody(
                        toHttpExchangeBody(
                                request.getContentType(),
                                request.getResourceType() != null && request.getResourceType().isBinary(),
                                request.getBody(),
                                request.getBodyString(),
                                mask
                        )
                );

        toNameValues(request.getParams(), null)
                .forEach(parameter -> builder.addQuery(parameter.name(), parameter.value()));

        return builder.build();
    }

    private static HttpExchangeResponse toHttpExchangeResponse(final HttpResponse response, final LogMask mask) {
        return HttpExchangeResponse.builder()
                .setStatus(response.getStatus())
                .setStatusText(response.getStatusText())
                .addHeaders(toNameValues(response.getHeaders(), mask))
                .setBody(
                        toHttpExchangeBody(
                                response.getContentType(),
                                response.getResourceType() != null && response.getResourceType().isBinary(),
                                response.getBodyBytes(),
                                response.getBodyString(),
                                mask
                        )
                )
                .build();
    }

    private static HttpExchangeBody toHttpExchangeBody(final String contentType,
                                                       final boolean binary,
                                                       final byte[] data,
                                                       final String text,
                                                       final LogMask mask) {
        if (Objects.isNull(data)) {
            return null;
        }
        final String encoding = binary ? "base64" : "utf8";
        final String originalValue = Objects.nonNull(text)
                ? text
                : new String(data, StandardCharsets.UTF_8);
        final String value = binary
                ? Base64.getEncoder().encodeToString(data)
                : Objects.isNull(mask) ? originalValue : mask.maskBody(originalValue);
        return new HttpExchangeBody(
                contentType,
                encoding,
                value,
                (long) data.length,
                false,
                null,
                null,
                null
        );
    }

    private static List<HttpExchangeNameValue> toNameValues(final Map<String, List<String>> values,
                                                            final LogMask mask) {
        if (Objects.isNull(values)) {
            return List.of();
        }
        final List<HttpExchangeNameValue> result = new ArrayList<>();
        values.forEach((name, items) -> {
            if (Objects.nonNull(items)) {
                items.forEach(
                        value -> result.add(
                                new HttpExchangeNameValue(
                                        name,
                                        Objects.isNull(mask) ? value : mask.maskHeader(name, value)
                                )
                        )
                );
            }
        });
        return result;
    }

    private void addEmbed(final AllureExternalKey stepKey, final StepResult.Embed embed) {
        try {
            if (isImageDiff(embed)) {
                addImageDiff(stepKey, embed);
            } else {
                addEmbedParts(stepKey, embed);
            }
            addEmbedMetadata(stepKey, embed);
        } catch (RuntimeException e) {
            LOGGER.warn("could not save embedding", e);
        }
    }

    private void addImageDiff(final AllureExternalKey stepKey, final StepResult.Embed embed) {
        final Map<String, StepResult.Part> parts = embed.getParts().stream()
                .collect(Collectors.toMap(StepResult.Part::getRole, part -> part));
        final Map<String, String> imageDiff = new LinkedHashMap<>();
        imageDiff.put("expected", toDataUrl(parts.get("baseline")));
        imageDiff.put("actual", toDataUrl(parts.get("current")));
        imageDiff.put("diff", toDataUrl(parts.get("diff")));
        addAttachment(
                stepKey,
                getEmbedName(embed),
                IMAGE_DIFF_CONTENT_TYPE,
                Json.toBytes(imageDiff)
        );
    }

    private void addEmbedParts(final AllureExternalKey stepKey, final StepResult.Embed embed) {
        final List<StepResult.Part> parts = embed.getParts();
        for (int index = 0; index < parts.size(); index++) {
            final StepResult.Part part = parts.get(index);
            final String role = Objects.isNull(part.getRole()) || part.getRole().isBlank()
                    ? "part-" + (index + 1)
                    : part.getRole();
            final boolean primary = parts.size() == 1 && "primary".equals(role);
            final String name = primary ? getEmbedName(embed) : getEmbedName(embed) + " [" + role + "]";
            if (Objects.nonNull(part.getData())) {
                addAttachment(
                        stepKey,
                        name,
                        Objects.requireNonNullElse(part.getMime(), BINARY_CONTENT_TYPE),
                        part.getData()
                );
            } else if (Objects.nonNull(part.getUrl())) {
                addAttachment(
                        stepKey,
                        name,
                        URI_LIST_CONTENT_TYPE,
                        (part.getUrl() + "\n").getBytes(StandardCharsets.UTF_8)
                );
            }
        }
    }

    private void addEmbedMetadata(final AllureExternalKey stepKey, final StepResult.Embed embed) {
        if (Objects.isNull(embed.getMeta()) || embed.getMeta().isEmpty()) {
            return;
        }
        addAttachment(
                stepKey,
                getEmbedName(embed) + " metadata",
                JSON_CONTENT_TYPE,
                Json.toBytes(embed.getMeta())
        );
    }

    private void addAttachment(final AllureExternalKey stepKey,
                               final String name,
                               final String type,
                               final byte[] data) {
        lifecycle.addAttachment(
                stepKey,
                name,
                type,
                new ByteArrayInputStream(data),
                AttachmentOptions.empty()
        );
    }

    private static boolean isImageDiff(final StepResult.Embed embed) {
        if (embed.getParts().size() != 3) {
            return false;
        }
        final Map<String, StepResult.Part> parts = embed.getParts().stream()
                .filter(part -> Objects.nonNull(part.getRole()))
                .collect(
                        Collectors.toMap(
                                StepResult.Part::getRole,
                                part -> part,
                                (first, second) -> first
                        )
                );
        return Stream.of("baseline", "current", "diff")
                .map(parts::get)
                .allMatch(part -> Objects.nonNull(part) && Objects.nonNull(part.getData()));
    }

    private static String toDataUrl(final StepResult.Part part) {
        final String mime = Objects.requireNonNullElse(part.getMime(), BINARY_CONTENT_TYPE);
        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(part.getData());
    }

    private static String getEmbedName(final StepResult.Embed embed) {
        return Objects.isNull(embed.getName()) || embed.getName().isBlank()
                ? "attachment"
                : embed.getName();
    }

    private static Status getStatus(final boolean failed, final Throwable error) {
        return failed
                ? Optional.ofNullable(error).flatMap(ResultsUtils::getStatus).orElse(null)
                : Status.PASSED;
    }

    private static StatusDetails getStatusDetails(final boolean failed,
                                                  final Throwable error,
                                                  final boolean redact) {
        if (!failed) {
            return null;
        }
        if (redact) {
            return new StatusDetails().setMessage(ScenarioResult.SUPPRESSED_FAILURE_MESSAGE);
        }
        return Optional.ofNullable(error).flatMap(ResultsUtils::getStatusDetails).orElse(null);
    }

    private static String getStepName(final Step step) {
        if (Objects.isNull(step.getKeyword())) {
            return step.getText();
        }
        if (Objects.isNull(step.getText()) || step.getText().isBlank()) {
            return step.getKeyword();
        }
        return step.getKeyword() + " " + step.getText();
    }

    private static List<String> getTagTexts(final Scenario scenario) {
        return scenario.getTagsEffective().stream()
                .map(Tag::getText)
                .toList();
    }

    private List<Label> getLabels(final List<String> labels) {
        final Map<String, String> allureLabels = new HashMap<>();
        final List<Label> allLabels = new ArrayList<>();
        for (String tag : labels.stream()
                .filter(l -> l.contains("allure")).collect(Collectors.toList())) {
            final String tagName = tag.substring(0, tag.indexOf(':'));
            final String tagValue = tag.substring(tag.indexOf(':') + 1);
            if (tagName.contains("allure.label")) {
                allureLabels.put(
                        tagName.substring("allure.label.".length()),
                        tagValue
                );
            }
            if (tagName.contains("allure.id")) {
                allureLabels.put("AS_ID", tagValue);
            }
            if (tagName.contains("allure.severity")) {
                allureLabels.put("severity", tagValue);
            }
        }
        allureLabels.keySet().forEach(key -> allLabels.add(createLabel(key, allureLabels.get(key))));
        return allLabels;
    }

    private List<Link> getLinks(final List<String> labels) {
        final List<Link> allureLinks = new ArrayList<>();
        for (String tag : labels.stream()
                .filter(l -> l.contains("allure.link")).collect(Collectors.toList())) {
            final String tagName = tag.substring(0, tag.indexOf(':'));
            final String tagValue = tag.substring(tag.indexOf(':') + 1);
            switch (tagName.substring("allure.link".length())) {
                case "":
                    allureLinks.add(createLink(tagValue, "", "", "custom"));
                    break;
                case ".tms":
                    allureLinks.add(createLink(tagValue, "", "", "tms"));
                    break;
                case ".issue":
                    allureLinks.add(createLink(tagValue, "", "", "issue"));
                    break;
                default:
                    break;
            }
        }
        return allureLinks;
    }

    private record ScenarioContext(
            String testUuid,
            AllureExternalKey ownerKey,
            AllureExternalKey scenarioKey,
            AllureExternalKey parentStepKey,
            ScenarioRuntime callerRuntime,
            boolean topLevel,
            boolean reportDisabled) {
    }
}
