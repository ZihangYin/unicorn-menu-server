package com.unicorn.rest.utils;

import java.util.UUID;

import javax.annotation.Nonnull;

public class UUIDGenerator {

    public static @Nonnull UUID randomUUID() {
        String secureRandom = UUID.randomUUID().toString();
        return UUID.fromString(UUID.nameUUIDFromBytes(secureRandom.getBytes()).toString());
    }
}
