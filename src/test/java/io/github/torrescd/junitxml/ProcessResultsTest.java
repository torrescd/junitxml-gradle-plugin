package io.github.torrescd.junitxml;

import org.junit.jupiter.api.Assertions;

class ProcessResultsTest {

    @org.junit.jupiter.api.Test
    void processResults() throws Exception {

        
        //ProcessResults.ProcessResults(testResultProcessor);
        
    }

    @org.junit.jupiter.api.Test
    void processResults1() throws Exception {


        Assertions.assertEquals(1,1, "ok");

    }

    @org.junit.jupiter.api.Test
    void processResults2() throws Exception {


        Assertions.assertEquals(2,2, "ok2");
        Assertions.assertEquals(3,4, "ok3");


    }
}