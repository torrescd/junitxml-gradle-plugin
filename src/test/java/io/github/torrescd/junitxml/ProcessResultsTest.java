package io.github.torrescd.junitxml;

import io.github.torrescd.junitxml.model.Report;
import org.gradle.api.internal.tasks.testing.TestResultProcessor;
import org.gradle.internal.operations.BuildOperationRef;

import java.io.File;

import static io.github.torrescd.junitxml.ProcessTestResultsTask.sendTestEvents;
import static org.mockito.Mockito.mock;

class ProcessResultsTest {


    @org.junit.jupiter.api.Test
    void processResults() throws Exception {


        new JunitParser().parse(new File[]{new File("src/test/resources/failure.xml")}, true);
        
        
    }
    @org.junit.jupiter.api.Test
    void processResults2() throws Exception {
        
        Report asd = new JunitParser().parse(new File[]{new File("src/test/resources/ITTCxUnitAll.xml")}, false);
        sendTestEvents(mock(BuildOperationRef.class), mock(TestResultProcessor.class), "", asd);

        return;

    }

}