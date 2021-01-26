package io.github.torrescd.junitxml;

import io.github.torrescd.junitxml.model.UnitTestCase;
import io.github.torrescd.junitxml.model.UnitTestSuite;
import io.github.torrescd.junitxml.model.Report;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class ShiftResultsOneLevel {

    public static Report process(Report report )
    {
        HashMap<String, UnitTestSuite>  testSuiteHashMap = new HashMap<>();

        Report newReport = new Report();
        
        for(UnitTestSuite testSuite : report.testsuite)
        {
            getOrAddTestSuitForMethod(testSuite, testSuiteHashMap);
        }

        newReport.testsuite.addAll(testSuiteHashMap.values());
           
        return newReport;
    }

    static UnitTestSuite getOrAddTestSuitForMethod(UnitTestSuite unitTestSuiteOld, HashMap<String, UnitTestSuite> testSuiteHashMap)
    {
        String method = unitTestSuiteOld.name;
        
        String[] parts = method.split("\\.");
        String className = Arrays.stream(parts).limit(parts.length - 1).collect(Collectors.joining("."));

        UnitTestSuite unitTestSuite;
        
        if (testSuiteHashMap.containsKey(className)) {
            unitTestSuite = testSuiteHashMap.get(className);
            unitTestSuite.tests++;
        }
        else
        {
            unitTestSuite = new UnitTestSuite();
            unitTestSuite.name = className;
            unitTestSuite.tests = 0;
            testSuiteHashMap.put(className, unitTestSuite);
        }
        
        
        UnitTestCase unitTestCase = new UnitTestCase();

        unitTestSuiteOld.testCases.forEach( oldTestCase -> unitTestCase.failures.addAll(oldTestCase.failures));
        
        unitTestCase.name = parts[parts.length -1];
        unitTestCase.time = unitTestSuiteOld.time;


        unitTestCase.properties.addAll(unitTestSuiteOld.properties);
                
        unitTestSuite.testCases.add(unitTestCase);
        
        return unitTestSuite;
        
    }

}
