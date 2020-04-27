package io.github.torrescd.junitxml;

import io.github.torrescd.junitxml.model.Report;
import io.github.torrescd.junitxml.model.SuppressableStacktraceException;
import io.github.torrescd.junitxml.model.UnitTestCase;
import io.github.torrescd.junitxml.model.UnitTestSuite;
import org.gradle.api.internal.tasks.testing.*;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.options.Option;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.TestResult;
import org.gradle.internal.id.IdGenerator;
import org.gradle.internal.id.LongIdGenerator;
import org.gradle.internal.operations.BuildOperationExecutor;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Files;
import java.util.stream.Collectors;

@CacheableTask
public class ProcessTestResultsTask extends Test {
    
    public void setAlreadyProcessed(boolean alreadyProcessed) {
        this.alreadyProcessed = alreadyProcessed;
    }

    @Input
    @Optional
    @Option(option = "alreadyProcessed", description = "")
    private boolean alreadyProcessed = false;
        
    @Override
    protected JvmTestExecutionSpec createTestExecutionSpec() {

        return new JvmTestExecutionSpec(null, null, null, false, null,  getPath(),null, 0, null, 0, null);
    }
    
    public void ProcessResults(TestResultProcessor testResultProcessor, String rootPath) throws Exception {
        
        getProject().getLogger().quiet("Searching test results on " + getCandidateClassFiles().getAsPath());
        
        File[] files = getCandidateClassFiles().getFiles().stream()
                .filter(name -> name.getName().toLowerCase().endsWith(".xml"))
                .collect(Collectors.toList()).toArray(new File[0]);
        
        //TODO finish this
        Report report = new JunitParser().parse(files, alreadyProcessed);
        
        File binFile = new File(getProject().getBuildDir(), "sample").getAbsoluteFile();
        Files.createDirectories(binFile.toPath());
        
        BuildOperationExecutor buildOperationExecutor = getServices().get(BuildOperationExecutor.class);
        
        if (buildOperationExecutor.getCurrentOperation().getParentId() == null)
            throw new RuntimeException("Null parent id");

        
        RootTestSuiteDescriptor rootTestSuiteDescriptor = new RootTestSuiteDescriptor(
                rootPath, 
                "cacheTest", 
                buildOperationExecutor.getCurrentOperation().getParentId());


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
                        ProcessResults( testResultProcessor, testExecutionSpec.getPath());

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


}
