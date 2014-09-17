package com.unicorn.rest.server.filter.model;

import javax.annotation.Nonnull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@Getter
public class Permission {

    private final String action;
    private final String resourceIdentifier;
    private final Long principal;
    
    public Permission(@Nonnull String action, @Nonnull String resourceIdentifier, @Nonnull Long principal) {
        this.action = action;
        this.resourceIdentifier = resourceIdentifier;
        this.principal = principal;
    }
    
}
