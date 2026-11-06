package com.breakinblocks.bblink.forge.common;

import com.breakinblocks.bblink.core.api.MinecraftService;
import com.breakinblocks.bblink.core.api.PlayerCountInfo;
import com.breakinblocks.bblink.forge.BBLinkForge;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import java.util.function.Consumer;

/**
 * Handles messages and commands coming from Discord and from linked servers. Those arrive on background threads,
 * so everything is handed to the server thread first.
 */
public class ForgeMinecraftService implements MinecraftService {
    @Override
    public PlayerCountInfo getPlayerCountInfo() {
        MinecraftServer server = BBLinkForge.getServer();
        if (server == null) return new PlayerCountInfo(0, 0);
        return new PlayerCountInfo(server.getCurrentPlayerCount(), server.getMaxPlayers());
    }

    @Override
    public void onMessage(String message) {
        BBLinkForge.runOnServerThread(server ->
                server.getConfigurationManager().sendChatMsgImpl(new ChatComponentText(message), false));
    }

    @Override
    public void onCommand(boolean isStaff, String name, String displayName, String fullCommand, Consumer<String> response) {
        BBLinkForge.runOnServerThread(server -> {
            final int opLevel = isStaff ? server.getOpPermissionLevel() : 0;
            ICommandSender sender = new ICommandSender() {
                @Override
                public String getCommandSenderName() {
                    return name;
                }

                @Override
                public IChatComponent func_145748_c_() {
                    return new ChatComponentText(displayName);
                }

                @Override
                public void addChatMessage(IChatComponent component) {
                    response.accept(getFormattedText(component));
                }

                @Override
                public boolean canCommandSenderUseCommand(int permLevel, String command) {
                    return opLevel >= permLevel;
                }

                @Override
                public ChunkCoordinates getPlayerCoordinates() {
                    return new ChunkCoordinates(0, 0, 0);
                }

                @Override
                public World getEntityWorld() {
                    return server.getEntityWorld();
                }
            };
            server.getCommandManager().executeCommand(sender, fullCommand);
        });
    }

    /**
     * Server side version of {@link IChatComponent#getFormattedText()} (which is client only), so command output
     * keeps bold/italic etc. when converted to Discord markdown.
     */
    static String getFormattedText(IChatComponent component) {
        StringBuilder builder = new StringBuilder();
        for (Object o : component) {
            IChatComponent part = (IChatComponent) o;
            builder.append(getFormattingCode(part.getChatStyle()));
            builder.append(part.getUnformattedTextForChat());
            builder.append(EnumChatFormatting.RESET);
        }
        return builder.toString();
    }

    /**
     * Server side version of {@link ChatStyle#getFormattingCode()}.
     */
    private static String getFormattingCode(ChatStyle style) {
        if (style.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        if (style.getColor() != null) builder.append(style.getColor());
        if (style.getBold()) builder.append(EnumChatFormatting.BOLD);
        if (style.getItalic()) builder.append(EnumChatFormatting.ITALIC);
        if (style.getUnderlined()) builder.append(EnumChatFormatting.UNDERLINE);
        if (style.getObfuscated()) builder.append(EnumChatFormatting.OBFUSCATED);
        if (style.getStrikethrough()) builder.append(EnumChatFormatting.STRIKETHROUGH);
        return builder.toString();
    }
}
