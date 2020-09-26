package com.breakinblocks.bbchat;

import com.breakinblocks.bbchat.common.BBChatConfig;
import com.breakinblocks.bbchat.common.ChatRelay;
import com.breakinblocks.bbchat.common.DummyRelay;
import com.breakinblocks.bbchat.common.IRelay;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.login.LoginException;
import java.util.stream.Collectors;

@Mod(BBChat.MODID)
public class BBChat {
    public static final String MODID = "bbchat";
    private static final Logger LOGGER = LogManager.getLogger();
    private IRelay relay = DummyRelay.INSTANCE;

    public BBChat() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BBChatConfig.commonSpec);
        // Ignore this mod being installed on either side
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void relayInit(FMLServerStartingEvent event) {
        try {
            final MinecraftServer server = event.getServer();
            relay = new ChatRelay(
                    server,
                    (msg) -> server.getPlayerList().sendMessage(new StringTextComponent(msg), false),
                    BBChatConfig.COMMON.botToken.get(),
                    BBChatConfig.COMMON.guildId.get(),
                    BBChatConfig.COMMON.channelId.get(),
                    BBChatConfig.COMMON.staffRoleId.get(),
                    BBChatConfig.COMMON.commandPrefix.get(),
                    BBChatConfig.COMMON.anyCommands.get().stream().map(String::toString).collect(Collectors.toList()),
                    BBChatConfig.COMMON.staffCommands.get().stream().map(String::toString).collect(Collectors.toList())
            );
        } catch (LoginException e) {
            LOGGER.warn("Failed to login ;-;. Check your bot token.", e);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void relayCleanup(FMLServerStoppedEvent event) {
        relay.cleanup();
        relay = DummyRelay.INSTANCE;
    }

    @SubscribeEvent
    public void relayServerStarted(FMLServerStartedEvent event) {
        relay.onStarted();
    }

    @SubscribeEvent
    public void relayServerStopped(FMLServerStoppedEvent event) {
        relay.onStopped();
    }

    @SubscribeEvent
    public void relayChat(ServerChatEvent event) {
        String name = event.getPlayer().getName().getUnformattedComponentText();
        String text = event.getMessage();
        relay.onChat(name, text);
    }

    @SubscribeEvent
    public void relayLogin(PlayerEvent.PlayerLoggedInEvent event) {
        String name = event.getPlayer().getName().getUnformattedComponentText();
        relay.onLogin(name);
    }

    @SubscribeEvent
    public void relayLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        String name = event.getPlayer().getName().getUnformattedComponentText();
        relay.onLogout(name);
    }

    @SubscribeEvent
    public void relayAchievement(AdvancementEvent event) {
        Advancement advancement = event.getAdvancement();
        DisplayInfo displayInfo = advancement.getDisplay();
        if (displayInfo == null) return;
        String name = event.getPlayer().getName().getUnformattedComponentText();
        String title = displayInfo.getTitle().getUnformattedComponentText();
        String description = displayInfo.getDescription().getUnformattedComponentText();
        relay.onAchievement(name, title, description);
    }
}
