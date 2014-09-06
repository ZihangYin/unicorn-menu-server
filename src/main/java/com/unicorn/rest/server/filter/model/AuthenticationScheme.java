package com.unicorn.rest.server.filter.model;

public enum AuthenticationScheme {
    
    BASIC_AUTHENTICATION("Basic "),
    BEARER_AUTHENTICATION("Bearer ");

    private String authSchemePrefix;

    private AuthenticationScheme(String authSchemePrefix) {
        this.authSchemePrefix = authSchemePrefix;
    }

    @Override
    public String toString() {
        return this.authSchemePrefix;
    }
}