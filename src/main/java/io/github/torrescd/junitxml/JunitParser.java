package io.github.torrescd.junitxml;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.torrescd.junitxml.model.Report;
import io.github.torrescd.junitxml.model.UnitTestSuite;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class JunitParser {
    
    Report parse(File[] files, boolean alreadyProcessed) throws IOException {

        Report report = new Report();
        for (File file : files) {

            JacksonXmlModule module = new JacksonXmlModule();

            //this is necessary
            module.setDefaultUseWrapper(false);

            XmlMapper mapper = new XmlMapper(module);
            
            //this is attached to our test fwk
            if (!alreadyProcessed) {
                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    report.testsuite.addAll(mapper.readValue(fileInputStream, Report.class).testsuite);
                }
                
            }
            else {
                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    report.testsuite.add(mapper.readValue(fileInputStream, UnitTestSuite.class));
                }
            }
            
        }

        if (!alreadyProcessed) {
            report = ShiftResultsOneLevel.process(report);
        }
        
        return report;
    }
}
