package io.github.torrescd.junitxml.deprecated;

import io.github.torrescd.junitxml.model.UnitFailure;
import io.github.torrescd.junitxml.model.UnitTestSuite;
import org.gradle.api.internal.tasks.testing.*;
import org.gradle.api.internal.tasks.testing.results.DefaultTestResult;
import org.gradle.api.tasks.testing.*;
import org.gradle.internal.event.ListenerManager;
import org.openmbee.junit.model.JUnitFailure;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UnusedCode extends AbstractTestTask {
    void processTestSuite(UnitTestSuite testSuite, Map buildResult) {

        ListenerManager listenerManager = getServices().get(ListenerManager.class);

        TestListener testListener = listenerManager.createAnonymousBroadcaster(TestListener.class).getSource();
        TestOutputListener testOutputListener = listenerManager.createAnonymousBroadcaster(TestOutputListener.class).getSource();


        String suiteName = testSuite.name;
        DecoratingTestDescriptor testSuiteDescriptor = new DecoratingTestDescriptor(new DefaultTestClassDescriptor(0, suiteName), createWorkerSuite());
        testListener.beforeSuite(testSuiteDescriptor.getParent().getParent());
        testListener.beforeSuite(testSuiteDescriptor.getParent());
        testListener.beforeSuite(testSuiteDescriptor);
        String timestamp = testSuite.timestamp;
        long startTime = 0L; //DatatypeFactory.newInstance().newXMLGregorianCalendar(timestamp).toGregorianCalendar().getTimeInMillis();
        testSuite.testCases.forEach( testCase ->
        {
            String testCaseClassName = testCase.classname;
            String testMethodName = testCase.name;
            DecoratingTestDescriptor testCaseDescriptor = new DecoratingTestDescriptor(new DefaultTestMethodDescriptor(0, testCaseClassName, testMethodName), testSuiteDescriptor);
            List<UnitFailure> failures = testCase.failures;
            long endTime = startTime + Math.round(testCase.time * 1000);

            if (failures.size() > 0) {
                testListener.beforeTest(testCaseDescriptor);
                //publishAdditionalMetadata(testCaseDescriptor, buildResult)
                try {
                    String systemErr = testSuite.systemErr;
                    if (systemErr != null) {
                        testOutputListener.onOutput(testCaseDescriptor, new DefaultTestOutputEvent(TestOutputEvent.Destination.StdErr, systemErr));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String failureText = failures.stream().map(UnitFailure::getValue).collect(Collectors.joining("\n"));

                failureText = failureText.replace("java.lang.AssertionError: ", "");
                testListener.afterTest(testCaseDescriptor,
                        new DefaultTestResult(
                                TestResult.ResultType.FAILURE, startTime, endTime, 1, 0, 1,
                                Collections.singletonList(new RuntimeException(failureText))));

            } else if (testCase.skipped == null) {
                testListener.beforeTest(testCaseDescriptor);
                //publishAdditionalMetadata(testCaseDescriptor, buildResult);
                testListener.afterTest(testCaseDescriptor,
                        new DefaultTestResult(TestResult.ResultType.SUCCESS, startTime, endTime, 1, 1,

                                0, Collections.emptyList()));
            }
        });

        testListener.afterSuite(testSuiteDescriptor,
                new DefaultTestResult(TestResult.ResultType.SUCCESS, 0, 0, 0, 0, 0, Collections.emptyList()));
        testListener.afterSuite(testSuiteDescriptor.getParent(),
                new DefaultTestResult(TestResult.ResultType.SUCCESS, 0, 0, 0, 0, 0, Collections.emptyList()));
        testListener.afterSuite(testSuiteDescriptor.getParent().getParent(),
                new DefaultTestResult(TestResult.ResultType.SUCCESS, 0, 0, 0, 0, 0, Collections.emptyList()));
    }

    private static TestDescriptorInternal createWorkerSuite() {
        return new DecoratingTestDescriptor(new DefaultTestSuiteDescriptor(0, "workerSuite"), createRootSuite());
    }

    private static TestDescriptorInternal createRootSuite() {
        return new DefaultTestSuiteDescriptor(0, "rootSuite");
    }

    @Override
    protected TestExecuter<? extends TestExecutionSpec> createTestExecuter() {
        return null;
    }

    @Override
    protected TestExecutionSpec createTestExecutionSpec() {
        return null;
    }
}
