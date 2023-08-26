package com.breakinblocks.bbchat.core.internal;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingException;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.meta.TypeQualifierDefault;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

class BBChatConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(BBChatConfig.class);

    @ConfigInfo
    String botToken = "<token>";

    @ConfigInfo
    String guildId = "0";

    @ConfigInfo
    String channelId = "0";

    @ConfigInfo(description = "Staff can run all commands with the OP level defined in server.properties.")
    String staffRoleId = "0";

    @ConfigInfo(description = "Commands must be prefixed with this (can also start with a direct mention).")
    String commandPrefix = "!";

    @ConfigInfo(description = "Anyone can use these commands. Will be run with OP Level 0 (non-operator) if not staff.")
    List<String> anyCommands = Arrays.asList("list", "forge");

    private final UnmodifiableCommentedConfig defaultConfig;
    private final List<ConfigLoader> configLoaders;

    BBChatConfig() {
        CommentedConfig defaultConfig = CommentedConfig.of(LinkedHashMap::new, TomlFormat.instance());
        this.defaultConfig = defaultConfig;
        configLoaders = new ArrayList<>();

        for (Field field : BBChatConfig.class.getDeclaredFields()) {
            try {
                ConfigInfo configInfo = field.getAnnotation(ConfigInfo.class);

                if (configInfo == null) {
                    continue;
                }

                String comment = configInfo.description();
                String name = field.getName();
                Object defaultValue = field.get(this);
                Class<?> type = field.getType();

                defaultConfig.set(name, defaultValue);
                defaultConfig.setComment(name, comment);

                configLoaders.add(newConfig -> {
                    boolean dirty = false;

                    if (!comment.equals(newConfig.getComment(name))) {
                        newConfig.setComment(name, comment);
                        dirty = true;
                    }

                    try {
                        Object newValue = newConfig.get(name);
                        if (newValue != null && type.isAssignableFrom(newValue.getClass())) {
                            field.set(this, newValue);
                        } else {
                            newConfig.set(name, defaultValue);
                            dirty = true;
                            LOGGER.warn("BBChatConfig: Invalid or missing value for '" + name + "' resetting to default.");
                            field.set(this, defaultValue);
                        }
                    } catch (IllegalAccessException e) {
                        LOGGER.warn("BBChatConfig: Failed to set config value '" + name + "'.", e);
                    }

                    return dirty;
                });
            } catch (IllegalAccessException e) {
                LOGGER.warn("BBChatConfig: Failed to initialize config value '" + field.getName() + "'.", e);
            }
        }

        Load();
    }

    private void Load() {
        try (CommentedFileConfig fileConfig = CommentedFileConfig
                .builder("config/bbchat.toml")
                .preserveInsertionOrder()
                .onFileNotFound(this::HandleNotFound)
                .build()
        ) {
            fileConfig.load();

            boolean dirty = false;
            for (ConfigLoader configLoader : configLoaders) {
                if (configLoader.load(fileConfig)) {
                    dirty = true;
                }
            }

            if (dirty) {
                fileConfig.save();
            }
        }
    }

    private boolean HandleNotFound(Path file, ConfigFormat<?> configFormat) {
        LOGGER.info("BBChatConfig: No config found at '" + file + "', writing default config there.");

        try {
            TomlWriter writer = new TomlWriter();
            writer.write(defaultConfig, file, WritingMode.REPLACE);
        } catch (WritingException e) {
            LOGGER.error("BBChatConfig: Failed to write default config to:" + file, e);
            return false;
        }

        return true;
    }

    @TypeQualifierDefault(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface ConfigInfo {
        /**
         * @return Config description.
         */
        String description() default "";
    }

    @FunctionalInterface
    private interface ConfigLoader {
        /**
         * Load value from config. If it doesn't exist create it.
         * Also updates the comment if it is wrong.
         *
         * @param config Config to load the value from.
         * @return true if changes were made to the config.
         */
        boolean load(CommentedConfig config);
    }
}
