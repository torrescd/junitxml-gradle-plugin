package io.github.torrescd.junitxml;

import java.io.File;

class ProcessResultsTest {


    @org.junit.jupiter.api.Test
    void processResults() throws Exception {


        new JunitParser().parse(new File[]{new File("src/test/resources/failure.xml")}, true);
        
        
    }


}