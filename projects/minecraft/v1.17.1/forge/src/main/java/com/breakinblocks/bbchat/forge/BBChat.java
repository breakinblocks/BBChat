package com.breakinblocks.bbchat.forge;

import com.breakinblocks.bbchat.core.internal.RelayEndpoint;
import com.breakinblocks.bbchat.core.internal.ChatRelay;
import com.breakinblocks.bbchat.core.internal.DummyRelay;
import com.breakinblocks.bbchat.core.api.PlayerCountInfo;
import net.minecraft.Util;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fmllegacy.network.FMLNetworkConstants;
import net.minecraftforge.fmlserverevents.FMLServerAboutToStartEvent;
import net.minecraftforge.fmlserverevents.FMLServerStartedEvent;
import net.minecraftforge.fmlserverevents.FMLServerStoppedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Mod(BBChat.MODID)
public class BBChat {
    public static final String MODID = "bbchat";
    private static final Logger LOGGER = LogManager.getLogger();
    private RelayEndpoint relay = DummyRelay.INSTANCE;
    private MinecraftServer server = null;

    public BBChat() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BBChatConfig.commonSpec);
        // Ignore this mod being installed on either side
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void relayInit(FMLServerAboutToStartEvent event) {
        server = event.getServer();
        relay = ChatRelay.create(
                BBChatConfig.COMMON.botToken.get(),
                BBChatConfig.COMMON.guildId.get(),
                BBChatConfig.COMMON.channelId.get(),
                BBChatConfig.COMMON.staffRoleId.get(),
                BBChatConfig.COMMON.commandPrefix.get(),
                BBChatConfig.COMMON.anyCommands.get().stream().map(String::toString).collect(Collectors.toList()),
                (msg) -> server.getPlayerList().broadcastMessage(new TextComponent(msg), ChatType.CHAT, Util.NIL_UUID),
                () -> new PlayerCountInfo(server.getPlayerCount(), server.getMaxPlayers()),
                this::handleCommand
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void relayCleanup(FMLServerStoppedEvent event) {
        relay.cleanup();
        relay = DummyRelay.INSTANCE;
        server = null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void relayServerStarted(FMLServerStartedEvent event) {
        relay.onStarted();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void relayServerStopped(FMLServerStoppedEvent event) {
        relay.onStopped();
    }

    @SubscribeEvent
    public void relayChat(ServerChatEvent event) {
        String name = event.getPlayer().getName().getString();
        String text = event.getMessage();
        relay.onChat(name, text);
    }

    @SubscribeEvent
    public void relayLogin(PlayerEvent.PlayerLoggedInEvent event) {
        String name = event.getPlayer().getName().getString();
        relay.onLogin(name);
    }

    @SubscribeEvent
    public void relayLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        String name = event.getPlayer().getName().getString();
        relay.onLogout(name);
    }

    @SubscribeEvent
    public void relayAchievement(AdvancementEvent event) {
        Advancement advancement = event.getAdvancement();
        DisplayInfo displayInfo = advancement.getDisplay();
        if (displayInfo == null) return;
        String name = event.getPlayer().getName().getString();
        String title = displayInfo.getTitle().getString();
        String description = displayInfo.getDescription().getString();
        relay.onAchievement(name, title, description);
    }

    /**
     * @see ServerPlayer#die(DamageSource)
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void relayDeath(LivingDeathEvent event) {
        final LivingEntity living = event.getEntityLiving();
        if (isRealPlayer(living) || (living.hasCustomName() && isRealPlayer(event.getSource().getEntity()))) {
            final Level world = living.getCommandSenderWorld();
            if (!world.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES)) return;
            String deathMessage = living.getCombatTracker().getDeathMessage().getString();
            String target = living.getName().getString();
            Entity sourceEntity = event.getSource().getEntity();
            String source = sourceEntity != null ? sourceEntity.getName().getString() : null;
            relay.onDeath(deathMessage, target, source);
        }
    }

    public boolean isRealPlayer(@Nullable Entity entity) {
        if (!(entity instanceof ServerPlayer)) return false;
        ServerPlayer player = (ServerPlayer) entity;
        if (player instanceof FakePlayer) return false;
        return player.connection != null;
    }

    private void handleCommand(boolean isStaff, String name, String displayName, String fullCommand, Consumer<String> response) {
        if (server == null) return;
        // Execute on the main server thread
        if (!server.isSameThread()) {
            server.execute(() -> handleCommand(isStaff, name, displayName, fullCommand, response));
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
                this.server, null
        );
        server.getCommands().performCommand(source, fullCommand);
    }

    @Nonnull
    private CommandSource getConsumerSource(Consumer<String> consumer) {
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
