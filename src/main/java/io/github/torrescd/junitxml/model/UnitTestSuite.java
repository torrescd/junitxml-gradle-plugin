package io.github.torrescd.junitxml.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.openmbee.junit.model.JUnitProperty;

import java.util.ArrayList;
import java.util.List;

public class UnitTestSuite {

    public UnitTestSuite()
    {
        
    }

    @JacksonXmlProperty(isAttribute = true)
    boolean disabled;
    @JacksonXmlProperty(isAttribute = true)
    int errors;
    
    @JacksonXmlProperty(isAttribute = true)
    int failures;
    @JacksonXmlProperty(isAttribute = true)
    String hostname;
    @JacksonXmlProperty(isAttribute = true)
    String id;

    @JacksonXmlProperty(isAttribute = true)
    public
    String name;

    @JacksonXmlProperty(isAttribute = true, localName = "package")
    String pakkage;
    
    @JacksonXmlProperty(isAttribute = true)
    int skipped;
    
    @JacksonXmlProperty(isAttribute = true)
    public
    int tests;
    
    @JacksonXmlProperty(isAttribute = true)
    public
    double time;
    
    @JacksonXmlProperty(isAttribute = true)
    public
    String timestamp;

    @JacksonXmlElementWrapper(localName = "properties")
    @JacksonXmlProperty(localName = "property")
    public List<JUnitProperty> properties = new ArrayList<>();

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "testcase")
    public List<UnitTestCase> testCases = new ArrayList<>();


    @JacksonXmlCData
    @JacksonXmlProperty(localName = "system-out")
    public String systemOut;

    @JacksonXmlCData
    @JacksonXmlProperty(localName = "system-err")
    public String systemErr;







}
