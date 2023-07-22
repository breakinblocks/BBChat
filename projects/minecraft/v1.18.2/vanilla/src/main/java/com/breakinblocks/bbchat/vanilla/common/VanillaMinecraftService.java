package com.breakinblocks.bbchat.vanilla.common;

import com.breakinblocks.bbchat.core.api.MinecraftService;
import com.breakinblocks.bbchat.core.api.PlayerCountInfo;
import com.breakinblocks.bbchat.vanilla.BBChat;
import net.minecraft.Util;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class VanillaMinecraftService implements MinecraftService {
    @Override
    public String getBotToken() {
        return BBChatConfig.COMMON.botToken.get();
    }

    @Override
    public String getGuildId() {
        return BBChatConfig.COMMON.guildId.get();
    }

    @Override
    public String getChannelId() {
        return BBChatConfig.COMMON.channelId.get();
    }

    @Override
    public String getStaffRoleId() {
        return BBChatConfig.COMMON.staffRoleId.get();
    }

    @Override
    public String getCommandPrefix() {
        return BBChatConfig.COMMON.commandPrefix.get();
    }

    @Override
    public Collection<String> getAnyCommands() {
        return BBChatConfig.COMMON.anyCommands.get().stream().map(String::toString).collect(Collectors.toList());
    }

    @Override
    public PlayerCountInfo getPlayerCountInfo() {
        MinecraftServer server = BBChat.INSTANCE.getServer();
        if (server == null) return new PlayerCountInfo(0, 0);
        return new PlayerCountInfo(server.getPlayerCount(), server.getMaxPlayers());
    }

    @Override
    public void onMessage(String message) {
        MinecraftServer server = BBChat.INSTANCE.getServer();
        if (server == null) return;
        server.getPlayerList().broadcastMessage(new TextComponent(message), ChatType.CHAT, Util.NIL_UUID);
    }

    @Override
    public void onCommand(boolean isStaff, String name, String displayName, String fullCommand, Consumer<String> response) {
        MinecraftServer server = BBChat.INSTANCE.getServer();
        if (server == null) return;
        // Execute on the main server thread
        if (!server.isSameThread()) {
            server.execute(() -> onCommand(isStaff, name, displayName, fullCommand, response));
            return;
        }
        // Create a command source with the correct level
        final int opLevel = isStaff ? server.getOperatorUserPermissionLevel() : 0;
        ServerLevel serverWorld = server.overworld();
        CommandSourceStack source = new CommandSourceStack(
                getConsumerSource(response),
                Vec3.atLowerCornerOf(serverWorld.getSharedSpawnPos()), Vec2.ZERO, serverWorld, // TODO: Make dynamic
                opLevel,
                name, new TextComponent(displayName),
                server, null
        );
        server.getCommands().performCommand(source, fullCommand);
    }

    @Nonnull
    private static CommandSource getConsumerSource(Consumer<String> consumer) {
        return new CommandSource() {
            @Override
            public void sendMessage(Component component, UUID senderUUID) {
                consumer.accept(component.getString());
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return true;
            }
        };
    }
}
