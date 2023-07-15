package com.breakinblocks.bbchat.core.api;

import java.util.ServiceLoader;

@SuppressWarnings("WeakerAccess")
public final class RelayUtils {
    private RelayUtils() {
    }

    public static <S> S loadSingleService(Class<S> service) {
        S firstServiceImpl = null;
        for (S serviceImpl : ServiceLoader.load(service)) {
            if (firstServiceImpl != null) {
                throw new RuntimeException("Expected a single service for " + service.getName() + " but more than one was found.");
            }

            firstServiceImpl = serviceImpl;
        }

        if (firstServiceImpl == null) {
            throw new RuntimeException("No implementation for " + service.getName() + " was found.");
        }

        return firstServiceImpl;
    }
}
