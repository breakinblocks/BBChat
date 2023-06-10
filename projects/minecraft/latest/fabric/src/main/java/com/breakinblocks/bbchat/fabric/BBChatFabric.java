package com.breakinblocks.bbchat.fabric;

import com.breakinblocks.bbchat.vanilla.BBChat;
import com.breakinblocks.bbchat.vanilla.common.BBChatConfig;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraftforge.fml.config.ModConfig;

public class BBChatFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ForgeConfigRegistry.INSTANCE.register(BBChat.MOD_ID, ModConfig.Type.COMMON, BBChatConfig.commonSpec);
    }
}
