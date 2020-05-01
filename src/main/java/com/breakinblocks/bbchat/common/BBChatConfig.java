package com.breakinblocks.bbchat.common;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.LongValue;
import org.apache.commons.lang3.tuple.Pair;

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
        }
    }
}
