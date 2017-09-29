package io.qameta.allure.spock.geb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spockframework.runtime.AbstractRunListener;
import org.spockframework.runtime.extension.IGlobalExtension;
import org.spockframework.runtime.model.SpecInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import geb.report.ReportState;
import geb.report.Reporter;
import geb.report.ReportingListener;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;

import static io.qameta.allure.spock.geb.GebFileTypes.HTML;
import static io.qameta.allure.spock.geb.GebFileTypes.PNG;
import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;


/**
 * @author Andreas Haardt
 */
@SuppressWarnings({
    "PMD.UnnecessaryFullyQualifiedName",
    "PMD.ExcessiveImports",
    "ClassFanOutComplexity",
    "PMD.CouplingBetweenObjects"
})
public class AllureSpockGeb extends AbstractRunListener implements IGlobalExtension, ReportingListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllureSpockGeb.class);

    private AllureLifecycle lifecycle;

    public AllureSpockGeb() {
        this(Allure.getLifecycle());
    }

    public AllureSpockGeb(final AllureLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Override
    public void start() {
        //do nothing at this point
    }

    @Override
    public void visitSpec(final SpecInfo spec) {
        spec.addSetupSpecInterceptor(new AllureSpockIntercepter(this));
    }

    @Override
    public void stop() {
        //do nothing at this point
    }

    @Override
    public void onReport(final Reporter reporter, final ReportState reportState, final List<File> reportFiles) {
        final Map<GebFileTypes, List<File>> supportedFiles = reportFiles.stream().filter(this::supportedExtensions)
            .collect(groupingBy(GebFileTypes::getFileTypeByFile,
                mapping(identity(), toList())));

        supportedFiles.getOrDefault(PNG, emptyList()).forEach(file ->
            createAttachment(file, PNG.getType(), PNG.getExtension()));

        supportedFiles.getOrDefault(HTML, emptyList()).forEach(file ->
            createAttachment(file, HTML.getType(), HTML.getExtension()));
    }

    private boolean supportedExtensions(final File file) {
        return PNG.matchExtension(file)
            || HTML.matchExtension(file);
    }

    private void createAttachment(final File file, final String type, final String extension) {
        try {
            lifecycle.addAttachment(file.getName(), type, extension, Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            LOGGER.error("Can't read geb report file: {}", file.getAbsoluteFile());
        }
    }
}
