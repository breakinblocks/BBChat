package com.breakinblocks.bbchat.common;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.LongValue;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class BBChatConfig {
    public static final ForgeConfigSpec commonSpec;
    public static final Common COMMON;

    static {
        final Pair<Common, ForgeConfigSpec> specPair = new Builder().configure(Common::new);
        commonSpec = specPair.getRight();
        COMMON = specPair.getLeft();
    }

    public static class Common {
        public final ConfigValue<String> botToken;
        public final LongValue guildId;
        public final LongValue channelId;
        public final LongValue staffRoleId;
        public final ConfigValue<String> commandPrefix;
        public final ConfigValue<List<? extends String>> anyCommands;

        public Common(Builder builder) {
            botToken = builder
                    .worldRestart()
                    .define("botToken", "<token>");
            guildId = builder
                    .worldRestart()
                    .defineInRange("guildId", 0L, Long.MIN_VALUE, Long.MAX_VALUE);
            channelId = builder
                    .worldRestart()
                    .defineInRange("channelId", 0L, Long.MIN_VALUE, Long.MAX_VALUE);
            staffRoleId = builder
                    .worldRestart()
                    .comment("Staff can run all commands with OP level defined in server.properties.")
                    .defineInRange("staffRoleId", 0L, Long.MIN_VALUE, Long.MAX_VALUE);
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
