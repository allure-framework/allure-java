package io.qameta.allure.testdata

import io.qameta.allure.AllureLifecycle
import io.qameta.allure.aspects.AttachmentsAspects
import io.qameta.allure.aspects.StepsAspects
import io.qameta.allure.model.TestResult
import io.qameta.allure.spock.AllureSpock
import io.qameta.allure.test.AllureResultsWriterStub
import org.junit.runner.notification.RunNotifier
import org.spockframework.runtime.JUnitDescriptionGenerator
import org.spockframework.runtime.RunContext
import org.spockframework.runtime.SpecInfoBuilder
import org.spockframework.runtime.model.SpecInfo


/**
 * Created on 14.06.2017
 *
 * @author Yuri Kudryavtsev
 *         skype: yuri.kudryavtsev.indeed
 *         email: yuri.kudryavtsev@indeed-id.com
 */

class AllureSpockRunner {

    private final static NOTIFIER = new RunNotifier()

    static List<TestResult> run(Class clazz) {
        AllureResultsWriterStub results = new AllureResultsWriterStub()
        AllureLifecycle lifecycle = new AllureLifecycle(results)

        StepsAspects.setLifecycle(lifecycle)
        AttachmentsAspects.setLifecycle(lifecycle)

        SpecInfo spec = new SpecInfoBuilder(clazz).build()
        spec.addListener(new AllureSpock(lifecycle))
        new JUnitDescriptionGenerator(spec).describeSpecMethods()
        new JUnitDescriptionGenerator(spec).describeSpec()
        RunContext.get().createSpecRunner(spec, NOTIFIER).run()

        return results.getTestResults()
    }

}
