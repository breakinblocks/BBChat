package com.breakinblocks.bbchat.forge;

import com.breakinblocks.bbchat.forge.common.ForgeMinecraftService;
import com.breakinblocks.bbchat.vanilla.BBChat;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fmllegacy.network.FMLNetworkConstants;
import net.minecraftforge.fmlserverevents.FMLServerAboutToStartEvent;
import net.minecraftforge.fmlserverevents.FMLServerStartedEvent;
import net.minecraftforge.fmlserverevents.FMLServerStartingEvent;
import net.minecraftforge.fmlserverevents.FMLServerStoppedEvent;
import net.minecraftforge.fmlserverevents.FMLServerStoppingEvent;

import javax.annotation.Nullable;

@Mod(BBChat.MOD_ID)
public class BBChatForge extends BBChat {
    public BBChatForge() {
        super(new ForgeMinecraftService());
        // Ignore this mod being installed on either side
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (FMLServerAboutToStartEvent event) -> setServer(event.getServer()));
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (FMLServerStartingEvent event) -> relayServerStarting());
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (FMLServerStartedEvent event) -> relayServerStarted());
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (FMLServerStoppingEvent event) -> relayServerStopping());
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, (FMLServerStoppedEvent event) -> relayServerStopped());
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
