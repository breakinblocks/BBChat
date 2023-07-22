package com.breakinblocks.bbchat.forge;

import com.breakinblocks.bbchat.forge.common.ForgeMinecraftService;
import com.breakinblocks.bbchat.vanilla.BBChat;
import com.breakinblocks.bbchat.vanilla.common.BBChatConfig;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.network.NetworkConstants;

import javax.annotation.Nullable;

@Mod(BBChat.MOD_ID)
public class BBChatForge extends BBChat {
    public BBChatForge() {
        super(new ForgeMinecraftService());
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BBChatConfig.commonSpec);
        // Ignore this mod being installed on either side
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (ServerAboutToStartEvent event) -> setServer(event.getServer()));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (ServerStartingEvent event) -> relayServerStarting());
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ServerStartedEvent event) -> relayServerStarted());
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (ServerStoppingEvent event) -> relayServerStopping());
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ServerStoppedEvent event) -> relayServerStopped());
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ServerChatEvent event) -> relayChat(event.getPlayer(), event.getMessage()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> relayLogin(event.getPlayer()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> relayLogout(event.getPlayer()));
        MinecraftForge.EVENT_BUS.addListener((AdvancementEvent event) -> relayAchievement(event.getPlayer(), event.getAdvancement()));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (LivingDeathEvent event) -> relayDeath(event.getEntityLiving(), event.getSource()));
    }

    @Override
    protected boolean isRealPlayer(@Nullable Entity entity) {
        return super.isRealPlayer(entity) && !(entity instanceof FakePlayer);
    }
}
