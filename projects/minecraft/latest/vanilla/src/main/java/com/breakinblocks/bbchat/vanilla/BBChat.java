package com.breakinblocks.bbchat.vanilla;

import com.breakinblocks.bbchat.core.api.MinecraftService;
import com.breakinblocks.bbchat.core.api.RelayService;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;

import javax.annotation.Nullable;

public class BBChat {
    public static final String MOD_ID = "bbchat";
    public static BBChat INSTANCE;

    private MinecraftServer server;

    public BBChat(MinecraftService minecraftService) {
        INSTANCE = this;
        RelayService.INSTANCE.setMinecraftService(minecraftService);
    }

    @Nullable
    public MinecraftServer getServer() {
        return server;
    }

    protected void setServer(MinecraftServer server) {
        this.server = server;
    }

    protected static void relayServerStarting() {
        RelayService.INSTANCE.onStarting();
    }

    protected static void relayServerStarted() {
        RelayService.INSTANCE.onStarted();
    }

    protected static void relayServerStopping() {
        RelayService.INSTANCE.onStopping();
    }

    protected static void relayServerStopped() {
        RelayService.INSTANCE.onStopped();
    }

    protected static void relayChat(ServerPlayer player, Component message) {
        String name = player.getName().getString();
        String text = message.getString();
        RelayService.INSTANCE.onChat(name, text);
    }

    protected static void relayLogin(Player player) {
        String name = player.getName().getString();
        RelayService.INSTANCE.onLogin(name);
    }

    protected static void relayLogout(Player player) {
        String name = player.getName().getString();
        RelayService.INSTANCE.onLogout(name);
    }

    protected static void relayAchievement(Player player, Advancement advancement) {
        DisplayInfo displayInfo = advancement.getDisplay();
        if (displayInfo == null) return;
        if (!displayInfo.shouldAnnounceChat()) return;
        if (!player.level().getGameRules().getBoolean(GameRules.RULE_ANNOUNCE_ADVANCEMENTS)) return;
        String name = player.getName().getString();
        String title = displayInfo.getTitle().getString();
        String description = displayInfo.getDescription().getString();
        RelayService.INSTANCE.onAchievement(name, title, description);
    }

    /**
     * @see ServerPlayer#die(DamageSource)
     */
    protected void relayDeath(LivingEntity livingEntity, DamageSource damageSource) {
        if (isRealPlayer(livingEntity) || (livingEntity.hasCustomName() && isRealPlayer(damageSource.getEntity()))) {
            if (!livingEntity.level().getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES)) return;
            String deathMessage = livingEntity.getCombatTracker().getDeathMessage().getString();
            String target = livingEntity.getName().getString();
            Entity sourceEntity = damageSource.getEntity();
            String source = sourceEntity != null ? sourceEntity.getName().getString() : null;
            RelayService.INSTANCE.onDeath(deathMessage, target, source);
        }
    }

    protected boolean isRealPlayer(@Nullable Entity entity) {
        if (!(entity instanceof ServerPlayer)) return false;
        ServerPlayer player = (ServerPlayer) entity;
        //noinspection ConstantValue
        return player.connection != null;
    }
}
