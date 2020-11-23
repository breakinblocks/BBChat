package com.breakinblocks.bbchat.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.File;

public class BBChatConfig {
    public static Configuration config;
    public static String botToken;
    public static String guildId;
    public static String channelId;
    public static String staffRoleId;
    public static String commandPrefix;
    public static String[] anyCommands;

    private static void sync() {
        botToken = config.getString(
                "botToken", Configuration.CATEGORY_GENERAL, "",
                "Discord bot token");
        guildId = config.getString(
                "guildId", Configuration.CATEGORY_GENERAL, "0",
                "Discord guild ID"
        );
        channelId = config.getString(
                "channelId", Configuration.CATEGORY_GENERAL, "0",
                "Discord channel ID"
        );
        staffRoleId = config.getString(
                "staffRoleId", Configuration.CATEGORY_GENERAL, "0",
                "Staff can run all commands with OP level defined in server.properties."
        );
        commandPrefix = config.getString(
                "commandPrefix", Configuration.CATEGORY_GENERAL, "!",
                "Commands must be prefixed with this (can also start with a direct mention)."
        );
        anyCommands = config.getStringList(
                "anyCommands", Configuration.CATEGORY_GENERAL, new String[]{"list", "forge"},
                "Anyone can use these commands. Will be run with OP Level 0 (non-operator) if not staff."
        );

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static void init(File suggestedConfigurationFile) {
        MinecraftForge.EVENT_BUS.register(new Listener());
        config = new Configuration(suggestedConfigurationFile);
        config.load();
        sync();
    }

    public static class Listener {
        @SubscribeEvent
        public void reloadConfig(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (!event.getModID().equals(BBChat.MODID)) return;
            sync();
        }
    }
}
