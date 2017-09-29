package io.qameta.allure.spock.geb;

import org.spockframework.runtime.extension.AbstractMethodInterceptor;
import org.spockframework.runtime.extension.IMethodInvocation;

import geb.Browser;
import geb.report.ReportingListener;
import geb.spock.GebSpec;

/**
 * The spock intercepter.
 *
 * @author Andreas Haardt
 */
public class AllureSpockIntercepter extends AbstractMethodInterceptor {

    private final ReportingListener reportingListener;

    public AllureSpockIntercepter(final ReportingListener reportingListener) {
        this.reportingListener = reportingListener;
    }

    @Override
    public void interceptSetupSpecMethod(final IMethodInvocation invocation) throws Throwable {
        final Object specInstance = invocation.getInstance();
        if (specInstance instanceof GebSpec) {
            final Browser browser = (Browser) ((GebSpec) specInstance).getBrowser();
            browser.getConfig().setReportingListener(reportingListener);
        }
        invocation.proceed();
    }
}
