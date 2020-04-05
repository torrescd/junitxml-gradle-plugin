package io.github.torrescd.junitxml.UnitTestCase;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.openmbee.junit.model.JUnitError;
import org.openmbee.junit.model.JUnitFailure;
import org.openmbee.junit.model.JUnitSkipped;

import javax.xml.bind.annotation.XmlAttribute;
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
    @XmlElement(name = "failure")
    public List<JUnitFailure> failures = new ArrayList<>();

    @XmlElement(name = "system-out")
    String systemOut;
    @XmlElement(name = "system-err")
    String systemErr;


}
