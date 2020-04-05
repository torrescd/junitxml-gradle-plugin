package io.github.torrescd.junitxml;

import io.github.torrescd.junitxml.UnitTestCase.UnitTestCase;
import io.github.torrescd.junitxml.UnitTestCase.UnitTestSuite;
import io.github.torrescd.junitxml.UnitTestCase.Report;

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
        
        unitTestCase.name = parts[parts.length -1];
        unitTestCase.time = unitTestSuiteOld.time;
                
        unitTestSuite.testCases.add(unitTestCase);
        
        return unitTestSuite;
        
    }

}
