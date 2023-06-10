package com.breakinblocks.bbchat.vanilla.common;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class BBChatConfig {
    public static final ForgeConfigSpec commonSpec;
    public static final Common COMMON;

    static {
        final Pair<Common, ForgeConfigSpec> specPair = new Builder().configure(Common::new);
        commonSpec = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    private BBChatConfig() {
    }

    public static class Common {
        public final ConfigValue<String> botToken;
        public final ConfigValue<String> guildId;
        public final ConfigValue<String> channelId;
        public final ConfigValue<String> staffRoleId;
        public final ConfigValue<String> commandPrefix;
        public final ConfigValue<List<? extends String>> anyCommands;

        Common(Builder builder) {
            botToken = builder
                    .worldRestart()
                    .define("botToken", "<token>");
            guildId = builder
                    .worldRestart()
                    .define("guildId", "0");
            channelId = builder
                    .worldRestart()
                    .define("channelId", "0");
            staffRoleId = builder
                    .worldRestart()
                    .comment("Staff can run all commands with OP level defined in server.properties.")
                    .define("staffRoleId", "0");
            commandPrefix = builder
                    .worldRestart()
                    .comment("Commands must be prefixed with this (can also start with a direct mention).")
                    .define("commandPrefix", "!");
            anyCommands = builder
                    .worldRestart()
                    .comment("Anyone can use these commands. Will be run with OP Level 0 (non-operator) if not staff.")
                    .defineList("anyCommands", Arrays.asList("list", "forge"), Objects::nonNull);
        }
    }
}
