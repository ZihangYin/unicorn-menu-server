package com.unicorn.rest.activity.model;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@XmlRootElement(name="error")
@JsonInclude(value=Include.NON_NULL)

@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor()
public class ErrorResponse {
    
    private static final String ERROR_TYPE = "error_type";
    private static final String ERROR_CODE = "error_code";
    private static final String ERROR_DESC = "error_desc";
    
    @JsonProperty(ERROR_TYPE)
    @Getter @Setter private String errorType;
    @JsonProperty(ERROR_CODE)
    @Getter @Setter private String errorCode;
    @JsonProperty(ERROR_DESC)
    @Getter @Setter private String errorDescription;
    
    @Override
    public String toString() {
        return "ErrorResponse [errorType=" + errorType + ", errorCode="
                + errorCode + ", errorDescription=" + errorDescription + "]";
    }
    
}
