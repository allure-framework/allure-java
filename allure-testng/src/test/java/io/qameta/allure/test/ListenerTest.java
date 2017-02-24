package io.qameta.allure.test;

import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import io.qameta.allure.testng.AllureTestNg;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.testng.ISuite;
import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.internal.ConstructorOrMethod;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class ListenerTest {

    private static final String STRING_PARAMETER = "String parameter";
    private AllureLifecycle lifecycle;
    private AllureTestNg allureTestNg;
    private static final String ALLURE_UUID_KEY = "ALLURE_UUID";
    private static final String RESULT_UUID = "Result uuid";
    private static final String CONTAINER_UUID = "Container uuid";
    private static final String SUITE_NAME = "Suite name";
    private static final String CLASS_NAME = "Class name";
    private static final String TEST_NAME = "Test name";
    private static final String QUALIFIED_NAME = "Test name";
    private static final String[] TEST_METHOD_PARAMETERS = new String[]{"Param1"};
    private static final String SUITE_UUID = "Suite uuid";
    private static final String CONTEXT_NAME = "Context name";

    @BeforeMethod
    public void prepare() {
        lifecycle = mock(AllureLifecycle.class, withSettings().verboseLogging());
        allureTestNg = new AllureTestNg(lifecycle);
    }

    @Test
    public void suiteStart() {
        ISuite isuite = mock(ISuite.class);
        when(isuite.getName()).thenReturn(SUITE_NAME);
        allureTestNg.onStart(isuite);

        ArgumentCaptor<TestResultContainer> suite = ArgumentCaptor.forClass(TestResultContainer.class);
        verify(lifecycle).startTestContainer(suite.capture());
        assertThat(suite.getValue())
                .extracting(TestResultContainer::getName).contains(SUITE_NAME);
    }

    @Test
    public void suiteFinish() {
        ISuite iSuite = mock(ISuite.class);
        when(iSuite.getAttribute(ALLURE_UUID_KEY)).thenReturn(SUITE_UUID);
        allureTestNg.onFinish(iSuite);

        InOrder order = inOrder(lifecycle);
        order.verify(lifecycle).stopTestContainer(SUITE_UUID);
        order.verify(lifecycle).writeTestContainer(SUITE_UUID);
    }

    @Test
    public void contextStart() {
        ISuite iSuite = mock(ISuite.class);
        when(iSuite.getAttribute(ALLURE_UUID_KEY)).thenReturn(SUITE_UUID);
        ITestContext iContext = mock(ITestContext.class);
        when(iContext.getAttribute(ALLURE_UUID_KEY)).thenReturn(CONTAINER_UUID);
        when(iContext.getName()).thenReturn(CONTEXT_NAME);
        when(iContext.getSuite()).thenReturn(iSuite);

        allureTestNg.onStart(iContext);
        ArgumentCaptor<TestResultContainer> contextContainer = ArgumentCaptor.forClass(TestResultContainer.class);
        verify(lifecycle).startTestContainer(eq(SUITE_UUID), contextContainer.capture());
        assertThat(contextContainer.getValue())
                .extracting(TestResultContainer::getUuid).contains(CONTAINER_UUID);
        assertThat(contextContainer.getValue())
                .extracting(TestResultContainer::getName).contains(CONTEXT_NAME);
    }

    @Test
    public void contextFinish() {
        ITestContext iContext = mock(ITestContext.class);
        when(iContext.getAttribute(ALLURE_UUID_KEY)).thenReturn(CONTAINER_UUID);

        allureTestNg.onFinish(iContext);
        InOrder order = inOrder(lifecycle);
        order.verify(lifecycle).stopTestContainer(CONTAINER_UUID);
        order.verify(lifecycle).writeTestContainer(CONTAINER_UUID);
    }

    private Method parameterizedMethod(String parameter) {
        try {
            return this.getClass().getDeclaredMethod("parameterizedMethod", String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void mockTestStart() {
        ITestResult iResult = mock(ITestResult.class);
        when(iResult.getAttribute(ALLURE_UUID_KEY)).thenReturn(RESULT_UUID);

        ITestContext iContext = mock(ITestContext.class);
        when(iContext.getAttribute(ALLURE_UUID_KEY)).thenReturn(CONTAINER_UUID);
        when(iResult.getTestContext()).thenReturn(iContext);

        ITestNGMethod method = mock(ITestNGMethod.class);
        ITestClass testClass = mock(ITestClass.class);
        XmlTest xmlTest = mock(XmlTest.class);
        XmlSuite suite = mock(XmlSuite.class);
        ConstructorOrMethod constructorOrMethod = mock(ConstructorOrMethod.class);

        when(iResult.getMethod()).thenReturn(method);
        when(iResult.getParameters()).thenReturn(TEST_METHOD_PARAMETERS);
        when(method.getTestClass()).thenReturn(testClass);
        when(method.getConstructorOrMethod()).thenReturn(constructorOrMethod);
        when(constructorOrMethod.getMethod()).thenReturn(parameterizedMethod(STRING_PARAMETER));
        when(method.getQualifiedName()).thenReturn(QUALIFIED_NAME);
        when(testClass.getXmlTest()).thenReturn(xmlTest);
        when(testClass.getName()).thenReturn(CLASS_NAME);
        when(xmlTest.getSuite()).thenReturn(suite);
        when(xmlTest.getName()).thenReturn(TEST_NAME);
        when(suite.getName()).thenReturn(SUITE_NAME);
        allureTestNg.onTestStart(iResult);
    }

    @Test
    public void testStart() throws NoSuchMethodException {
        mockTestStart();

        ArgumentCaptor<TestResult> result = ArgumentCaptor.forClass(TestResult.class);
        InOrder order = inOrder(lifecycle);
        order.verify(lifecycle).scheduleTestCase(eq(CONTAINER_UUID), result.capture());
        TestResult value = result.getValue();
        List<Label> labels = value.getLabels();
        List<io.qameta.allure.model.Parameter> parameters = value.getParameters();
        assertThat(labels).hasSize(7);
        assertThat(parameters)
                .hasSize(1)
                .extracting(io.qameta.allure.model.Parameter::getValue)
                .containsOnly(TEST_METHOD_PARAMETERS);
        order.verify(lifecycle).startTestCase(value.getUuid());
    }

    @Test
    public void testSuccess() throws NoSuchMethodException {
        mockTestStart();
        ArgumentCaptor<TestResult> result = ArgumentCaptor.forClass(TestResult.class);
        verify(lifecycle).scheduleTestCase(eq(CONTAINER_UUID), result.capture());

        ITestResult iResult = mock(ITestResult.class);
        allureTestNg.onTestSuccess(iResult);
        InOrder order = inOrder(lifecycle);
        final String uuid = result.getValue().getUuid();
        order.verify(lifecycle).updateTestCase(eq(uuid), any());
        order.verify(lifecycle).stopTestCase(uuid);
        order.verify(lifecycle).writeTestCase(uuid);
    }

    @Test
    public void testFailure() throws NoSuchMethodException {
        mockTestStart();
        ArgumentCaptor<TestResult> result = ArgumentCaptor.forClass(TestResult.class);
        verify(lifecycle).scheduleTestCase(eq(CONTAINER_UUID), result.capture());

        ITestResult iResult = mock(ITestResult.class);
        when(iResult.getThrowable()).thenReturn(new Throwable("Cause"));

        allureTestNg.onTestFailure(iResult);
        InOrder order = inOrder(lifecycle);
        final String uuid = result.getValue().getUuid();
        order.verify(lifecycle).updateTestCase(eq(uuid), any());
        order.verify(lifecycle).stopTestCase(uuid);
        order.verify(lifecycle).writeTestCase(uuid);
    }

    @Test
    public void testSkipped() throws NoSuchMethodException {
        mockTestStart();
        ArgumentCaptor<TestResult> result = ArgumentCaptor.forClass(TestResult.class);
        verify(lifecycle).scheduleTestCase(eq(CONTAINER_UUID), result.capture());

        ITestResult iResult = mock(ITestResult.class);

        allureTestNg.onTestSkipped(iResult);
        InOrder order = inOrder(lifecycle);
        final String uuid = result.getValue().getUuid();
        order.verify(lifecycle).updateTestCase(eq(uuid), any());
        order.verify(lifecycle).stopTestCase(uuid);
        order.verify(lifecycle).writeTestCase(uuid);
    }
}
