package io.github.torrescd.junitxml.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.openmbee.junit.model.JUnitError;
import org.openmbee.junit.model.JUnitSkipped;

import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

public class UnitTestCase {
    
    @JacksonXmlProperty(isAttribute = true)
    int assertions;

    @JacksonXmlProperty(isAttribute = true)
    public
    String classname;

    @JacksonXmlProperty(isAttribute = true)
    public String name;

    @JacksonXmlProperty(isAttribute = true)
    String status;
    
    @JacksonXmlProperty(isAttribute = true)
    public Double time;

    @XmlElement
    public JUnitSkipped skipped;
    
    @XmlElement(name = "error")
    List<JUnitError> errors;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "failure")
    public List<UnitFailure> failures = new ArrayList<>();

    @XmlElement(name = "system-out")
    String systemOut;
    @XmlElement(name = "system-err")
    String systemErr;


}
