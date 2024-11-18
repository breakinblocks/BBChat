package com.breakinblocks.bbchat.neo;

import com.breakinblocks.bbchat.neo.common.NeoMinecraftService;
import com.breakinblocks.bbchat.vanilla.BBChat;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import javax.annotation.Nullable;

@Mod(BBChat.MOD_ID)
public class BBChatNeo extends BBChat {
    public BBChatNeo() {
        super(new NeoMinecraftService());
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (ServerAboutToStartEvent event) -> setServer(event.getServer()));
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (ServerStartingEvent event) -> relayServerStarting());
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ServerStartedEvent event) -> relayServerStarted());
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, (ServerStoppingEvent event) -> relayServerStopping());
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ServerStoppedEvent event) -> relayServerStopped());
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (ServerChatEvent event) -> relayChat(event.getPlayer(), event.getMessage()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> relayLogin(event.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> relayLogout(event.getEntity()));
        NeoForge.EVENT_BUS.addListener((AdvancementEvent.AdvancementEarnEvent event) -> relayAchievement(event.getEntity(), event.getAdvancement()));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, (LivingDeathEvent event) -> relayDeath(event.getEntity(), event.getSource()));
    }

    @Override
    protected boolean isRealPlayer(@Nullable Entity entity) {
        return super.isRealPlayer(entity) && !(entity instanceof FakePlayer);
    }
}
