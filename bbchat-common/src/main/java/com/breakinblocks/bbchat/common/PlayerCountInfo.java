package com.breakinblocks.bbchat.common;

public final class PlayerCountInfo {
    private final int current;
    private final int max;

    public PlayerCountInfo(int current, int max) {
        this.current = current;
        this.max = max;
    }

    public int getCurrent() {
        return current;
    }

    public int getMax() {
        return max;
    }
}
