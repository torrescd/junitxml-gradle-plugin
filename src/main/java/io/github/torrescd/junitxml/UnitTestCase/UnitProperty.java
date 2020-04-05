package io.github.torrescd.junitxml.UnitTestCase;

import org.openmbee.junit.model.JUnitProperty;

import javax.xml.bind.annotation.XmlElement;
import java.util.List;

public class UnitProperty {

    @XmlElement(name = "property")
    List<JUnitProperty> property;
}
