package com.breakinblocks.bbchat.core.api;

@SuppressWarnings("WeakerAccess")
public final class RelayUtils {
    private RelayUtils() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T loadClass(String className) {
        try {
            return (T) Class.forName(className).newInstance();
        } catch (ClassCastException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
