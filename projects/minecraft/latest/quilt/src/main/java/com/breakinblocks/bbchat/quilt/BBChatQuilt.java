package com.breakinblocks.bbchat.quilt;

import com.breakinblocks.bbchat.vanilla.BBChat;
import com.breakinblocks.bbchat.vanilla.common.BBChatConfig;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.minecraftforge.fml.config.ModConfig;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;

public class BBChatQuilt implements ModInitializer {
    @Override
    public void onInitialize(ModContainer mod) {
        ForgeConfigRegistry.INSTANCE.register(BBChat.MOD_ID, ModConfig.Type.COMMON, BBChatConfig.commonSpec);
    }
}
