package io.github.torrescd.junitxml.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.Data;

@Data
public class UnitFailure {

    @JacksonXmlProperty(isAttribute = true)
    private String message;

    @JacksonXmlProperty(isAttribute = true)
    private String type;

    @JacksonXmlText
    private String value;
    
}
