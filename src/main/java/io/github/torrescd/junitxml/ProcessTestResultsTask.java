package io.github.torrescd.junitxml;

import io.github.torrescd.junitxml.model.Report;
import io.github.torrescd.junitxml.model.SuppressableStacktraceException;
import io.github.torrescd.junitxml.model.UnitTestCase;
import io.github.torrescd.junitxml.model.UnitTestSuite;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.tasks.testing.*;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.options.Option;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.TestResult;
import org.gradle.internal.actor.ActorFactory;
import org.gradle.internal.actor.internal.DefaultActorFactory;
import org.gradle.internal.concurrent.DefaultExecutorFactory;
import org.gradle.internal.concurrent.ExecutorFactory;
import org.gradle.internal.id.CompositeIdGenerator;
import org.gradle.internal.id.IdGenerator;
import org.gradle.internal.id.LongIdGenerator;
import org.gradle.internal.operations.BuildOperationExecutor;
import org.gradle.internal.operations.BuildOperationRef;
import org.gradle.internal.service.DefaultServiceRegistry;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.internal.time.Clock;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

@CacheableTask
public class ProcessTestResultsTask extends Test {

    public void setAlreadyProcessed(Boolean alreadyProcessed) {
        this.alreadyProcessed = alreadyProcessed;
    }
    
    public Boolean isAlreadyProcessed() {
        return alreadyProcessed;
    }


    @Input
    @Optional
    @Option(option = "alreadyProcessed", description = "")
    private Boolean alreadyProcessed = false;
        
    @Override
    protected JvmTestExecutionSpec createTestExecutionSpec() {

        return new JvmTestExecutionSpec(getTestFramework(), null, null, false, null,  getPath(),null, 0, null, 0, null);
    }
    
    public void ProcessResults(TestResultProcessor testResultProcessor, String rootPath) throws Exception {
        
        getProject().getLogger().quiet("Searching test results on " + getCandidateClassFiles().getAsPath());
        
        File[] files = getCandidateClassFiles().getFiles().stream()
                .filter(name -> name.getName().toLowerCase().endsWith(".xml"))
                .collect(Collectors.toList()).toArray(new File[0]);
        
        //TODO finish this
        Report report = new JunitParser().parse(files, alreadyProcessed);
        
        //maybe we want to export "property decorated" format to be treated by third party
        

        BuildOperationExecutor buildOperationExecutor = getServices().get(BuildOperationExecutor.class);
        
        if (buildOperationExecutor.getCurrentOperation().getParentId() == null)
            throw new RuntimeException("Null parent id");

        BuildOperationRef buildOperation = buildOperationExecutor.getCurrentOperation();

        //this regenerates standard junit format
        sendTestEvents(buildOperation, testResultProcessor, rootPath, report);
    }

    static public void sendTestEvents(BuildOperationRef buildOperation,
                                      TestResultProcessor testResultProcessor, String rootPath, Report report) throws IOException {
       
        RootTestSuiteDescriptor rootTestSuiteDescriptor = new RootTestSuiteDescriptor(
                rootPath,
                "cacheTest",
                buildOperation.getParentId());
        
        testResultProcessor.started(rootTestSuiteDescriptor, new TestStartEvent(0));

        IdGenerator<Long> idGenerator = new LongIdGenerator();

        for( UnitTestSuite testSuite: report.testsuite)
        {
            DefaultTestSuiteDescriptor testSuiteDescriptor = new DefaultTestSuiteDescriptor(idGenerator.generateId(), testSuite.name);
            DecoratingTestDescriptor decoratingTestSuiteDescriptor = new DecoratingTestDescriptor(testSuiteDescriptor, rootTestSuiteDescriptor);
            testResultProcessor.started(decoratingTestSuiteDescriptor, new TestStartEvent(0, rootTestSuiteDescriptor.getId()));

            for( UnitTestCase testCase: testSuite.testCases)
            {
                TestResult.ResultType resultType = testCase.failures.size() > 0 ? TestResult.ResultType.FAILURE : TestResult.ResultType.SUCCESS;
                resultType = testCase.skipped != null ? TestResult.ResultType.SKIPPED : resultType;

                DefaultTestDescriptor testCaseDescriptor = new DefaultTestDescriptor(idGenerator.generateId(), testSuite.name, testCase.name);
                
                DecoratingTestDescriptor decoratingTestDescriptor = new DecoratingTestDescriptor(testCaseDescriptor, testSuiteDescriptor);

                testResultProcessor.started(decoratingTestDescriptor, new TestStartEvent(0L, decoratingTestSuiteDescriptor.getId()));

                testCase.failures.forEach( failure -> testResultProcessor.failure(
                        decoratingTestDescriptor.getId(), 
                        new SuppressableStacktraceException(failure.getMessage())));

                testResultProcessor.completed(decoratingTestDescriptor.getId(), new TestCompleteEvent( Math.round(testCase.time * 1000), resultType));
            }

            testResultProcessor.completed(decoratingTestSuiteDescriptor.getId(), new TestCompleteEvent( Math.round(testSuite.time * 1000), TestResult.ResultType.SUCCESS));

        }

        testResultProcessor.completed(rootTestSuiteDescriptor.getId(), new TestCompleteEvent(0, TestResult.ResultType.SUCCESS) );
        //test.write(testClassResults);

    }


    TestExecuter testExecuter;

    @Override
    protected TestExecuter<JvmTestExecutionSpec> createTestExecuter() {
        
        if (testExecuter == null) {
            return new TestExecuter<JvmTestExecutionSpec>() {
                @Override
                public void execute(JvmTestExecutionSpec testExecutionSpec, TestResultProcessor testResultProcessor) {
     
                    try {

                        ProjectInternal project = (ProjectInternal) getProject();
                        WorkerTestClassProcessorFactory factory = getTestFramework().getProcessorFactory();

                        TestClassProcessor testClassProcessor = factory.create(new TestFrameworkServiceRegistry2(project.getServices()));

                        testClassProcessor.startProcessing(testResultProcessor);
                        
                        ProcessResults(testResultProcessor, testExecutionSpec.getPath());

                        testClassProcessor.stop();

                    } catch (Exception e) {
                        throw new RuntimeException(e);
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

    private static class TestFrameworkServiceRegistry2 extends DefaultServiceRegistry {

        private final ServiceRegistry serviceRegistry;
        
        private TestFrameworkServiceRegistry2(ServiceRegistry serviceRegistry) {
            this.serviceRegistry = serviceRegistry;
        }

        protected Clock createClock() {
            return serviceRegistry.get(Clock.class);
        }

        protected IdGenerator<Object> createIdGenerator() {
            return new CompositeIdGenerator("", new LongIdGenerator());
        }

        protected ExecutorFactory createExecutorFactory() {
            return new DefaultExecutorFactory();
        }

        protected ActorFactory createActorFactory(ExecutorFactory executorFactory) {
            return new DefaultActorFactory(executorFactory);
        }
    }


}
