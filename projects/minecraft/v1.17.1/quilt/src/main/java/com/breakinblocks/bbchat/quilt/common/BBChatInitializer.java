package com.breakinblocks.bbchat.quilt.common;

import org.quiltmc.loader.api.ModContainer;

public interface BBChatInitializer {
    public static final String ENTRYPOINT_KEY = "bbchat:init";

    void onInitialize(ModContainer mod);
}
