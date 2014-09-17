package com.unicorn.rest.server.filter.model;

import java.util.List;

import javax.annotation.Nonnull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public class AccessControlPolicy {
    
    private final Permission permission;
    /*
     *  TODO: As oppose to allowingConditions, we should support denying conditions as well in order 
     *  to overrides the allowing conditions.
     *  
     */
    private final List<String> allowingConditions;
    
    public AccessControlPolicy(@Nonnull Permission permission, @Nonnull List<String> allowingConditions) {
        this.permission = permission;
        this.allowingConditions = allowingConditions;
    }
}
