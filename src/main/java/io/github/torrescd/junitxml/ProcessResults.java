package io.github.torrescd.junitxml;

import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.torrescd.junitxml.UnitTestCase.Report;
import io.github.torrescd.junitxml.UnitTestCase.UnitTestCase;
import io.github.torrescd.junitxml.UnitTestCase.UnitTestSuite;
import org.gradle.api.internal.tasks.testing.*;
import org.gradle.api.internal.tasks.testing.detection.DefaultTestExecuter;
import org.gradle.api.internal.tasks.testing.junit.result.TestResultSerializer;
import org.gradle.api.internal.tasks.testing.processors.CaptureTestOutputTestResultProcessor;
import org.gradle.api.internal.tasks.testing.results.AttachParentTestResultProcessor;
import org.gradle.api.internal.tasks.testing.results.DefaultTestResult;
import org.gradle.api.tasks.testing.*;
import org.gradle.internal.event.ListenerManager;
import org.gradle.internal.impldep.org.apache.commons.lang.exception.ExceptionUtils;
import org.gradle.internal.operations.BuildOperationExecutor;
import org.openmbee.junit.model.JUnitFailure;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProcessResults extends Test {

    public ProcessResults()
    {
        super();
    }


    @Override
    protected JvmTestExecutionSpec createTestExecutionSpec() {

        return new JvmTestExecutionSpec(null, null, null, false, null,  getPath(),null, 0, null, 0, null);
    }


    Object getTaskPath(TestDescriptorInternal givenDescriptor) {
        TestDescriptorInternal descriptor = givenDescriptor;
        while (descriptor.getOwnerBuildOperationId() == null && descriptor.getParent() != null) {
            descriptor = descriptor.getParent();
        }
        return descriptor.getOwnerBuildOperationId();
    }

    public void ProcessResults(TestResultProcessor testResultProcessor, String rootPath) throws Exception {

        File dir = new File(getProject().getBuildDir(), "test-results/test");
        File[] files = dir.listFiles((dir1, name) -> name.toLowerCase().endsWith(".xml"));


        getProject().getLogger().quiet(files[0].getAbsolutePath());
        
        
        FileInputStream fileInputStream = new FileInputStream(files[0]);
        
        JacksonXmlModule module = new JacksonXmlModule();

        //this is necessary
        module.setDefaultUseWrapper(false);

        XmlMapper mapper = new XmlMapper(module);

        Report report = mapper.readValue(fileInputStream, Report.class);

        report = ShiftResultsOneLevel.process(report);
        
        File binFile = new File(getProject().getBuildDir(), "sample").getAbsoluteFile();
        Files.createDirectories(binFile.toPath());
        
        TestResultSerializer test = new TestResultSerializer(binFile);

        
        //List<TestClassResult> testClassResults = new ArrayList<>();
        
        getProject().getLogger().quiet(report.testsuite.size() + " size");

        BuildOperationExecutor buildOperationExecutor = getServices().get(BuildOperationExecutor.class);
        
        /*ExecutingBuildOperation operation = buildOperationExecutor.start(BuildOperationDescriptor
                .displayName("Snapshot task inputs for ")
                .name("Snapshot task inputs").details("asdasd"));*/
        
        if (buildOperationExecutor.getCurrentOperation().getParentId() == null)
            throw new RuntimeException("Null parent id");

        
        RootTestSuiteDescriptor rootTestSuiteDescriptor = new RootTestSuiteDescriptor(
                rootPath, 
                "cacheTest", 
                buildOperationExecutor.getCurrentOperation().getParentId());


        getProject().getLogger().quiet("preo");

        testResultProcessor.started(rootTestSuiteDescriptor, new TestStartEvent(0));

        getProject().getLogger().quiet("pro");


        for( UnitTestSuite testSuite: report.testsuite)
        {

            //TestClassResult testClassResult = new TestClassResult(1L, testSuite.name, 0L);
            //testClassResults.add(testClassResult);

            getProject().getLogger().quiet(testSuite.name);
            DefaultTestClassDescriptor defaultTestClassDescriptor = new DefaultTestClassDescriptor(testSuite, testSuite.name);
            
            DecoratingTestDescriptor testSuiteDescriptor = new DecoratingTestDescriptor(defaultTestClassDescriptor, rootTestSuiteDescriptor);

            Object id = getTaskPath(testSuiteDescriptor);
            if (id == null)
                throw new RuntimeException("idnull");
            
            testResultProcessor.started(testSuiteDescriptor, new TestStartEvent(0));
            
            
            for( UnitTestCase testCase: testSuite.testCases)
            {

                TestResult.ResultType resultType = testCase.failures.size() > 0 ? TestResult.ResultType.FAILURE : TestResult.ResultType.SUCCESS;
                resultType = testCase.skipped != null ? TestResult.ResultType.SKIPPED : resultType;
                
                /*
                TestMethodResult testMethodResult = new TestMethodResult(1L,
                        testCase.name,  
                        resultType,
                        Math.round(testCase.time * 1000),
                        Math.round(testCase.time * 1000));*/
                
                //testClassResult.add(testMethodResult);

                DecoratingTestDescriptor testCaseDescriptor = new DecoratingTestDescriptor(
                        new DefaultTestMethodDescriptor(testCase, testSuite.name, testCase.name), testSuiteDescriptor);
                testResultProcessor.started(testCaseDescriptor, new TestStartEvent(0L));
                
                testResultProcessor.completed(testCaseDescriptor.getId(), new TestCompleteEvent( Math.round(testCase.time * 1000), resultType));
            }
            
            testResultProcessor.completed(testSuiteDescriptor.getId(), new TestCompleteEvent( Math.round(testSuite.time * 1000)));

            //processTestSuite(testSuite, null);

        }

        testResultProcessor.completed(rootTestSuiteDescriptor.getId(), new TestCompleteEvent(0));


        //test.write(testClassResults);

    }

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
            List<JUnitFailure> failures = testCase.failures;
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
                String failureText = failures.stream().map(JUnitFailure::getValue).collect(Collectors.joining("\n"));
                
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

    TestExecuter testExecuter;


    @Override
    protected TestExecuter<JvmTestExecutionSpec> createTestExecuter() {

        
        if (testExecuter == null) {
            return new TestExecuter<JvmTestExecutionSpec>() {
                @Override
                public void execute(JvmTestExecutionSpec testExecutionSpec, TestResultProcessor testResultProcessor) {
                    try {

                        ProcessResults(new AttachParentTestResultProcessor(testResultProcessor), testExecutionSpec.getPath());
                        //ProcessResults(new  AttachParentTestResultProcessor(testResultProcessor));
                        //ProcessResults(testResultProcessor);

                    } catch (Exception e) {

                        getProject().getLogger().error("1");
                      getProject().getLogger().error(e.getMessage());
                        getProject().getLogger().error("2");
                        getProject().getLogger().error(ExceptionUtils.getStackTrace(e));
                        getProject().getLogger().error("3");

                        e.printStackTrace();
                    }
                }

                @Override
                public void stopNow() {

                }
            };
        } else {
            return testExecuter;
        }
    }

/*
    @Override
    protected TestExecuter<JvmTestExecutionSpec> createTestExecuter() {
        if (testExecuter == null) {
            return new DefaultTestExecuter(getProcessBuilderFactory(), getActorFactory(), getModuleRegistry(),
                    getServices().get(WorkerLeaseRegistry.class),
                    getServices().get(BuildOperationExecutor.class),
                    getServices().get(StartParameter.class).getMaxWorkerCount(),
                    getServices().get(Clock.class),
                    getServices().get(DocumentationRegistry.class),
                    (DefaultTestFilter) getFilter());
        } else {
            return testExecuter;
        }
    }*/

    public static final class RootTestSuiteDescriptor extends DefaultTestSuiteDescriptor {
        private final Object testTaskOperationId;
        

        private RootTestSuiteDescriptor(Object id, String name, Object testTaskOperationId) {
            super(id, name);
            this.testTaskOperationId = testTaskOperationId;
        }

        @Nullable
        @Override
        public Object getOwnerBuildOperationId() {
            return testTaskOperationId;
        }

        @Override
        public String toString() {
            return getName();
        }
    }


}
