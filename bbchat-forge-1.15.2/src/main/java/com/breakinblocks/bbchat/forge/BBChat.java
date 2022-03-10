package com.breakinblocks.bbchat.forge;

import com.breakinblocks.bbchat.common.ChatRelay;
import com.breakinblocks.bbchat.common.DummyRelay;
import com.breakinblocks.bbchat.common.IRelay;
import com.breakinblocks.bbchat.common.PlayerCountInfo;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Mod(BBChat.MODID)
public class BBChat {
    public static final String MODID = "bbchat";
    private static final Logger LOGGER = LogManager.getLogger();
    private IRelay relay = DummyRelay.INSTANCE;
    private MinecraftServer server = null;

    public BBChat() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BBChatConfig.commonSpec);
        // Ignore this mod being installed on either side
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void relayInit(FMLServerAboutToStartEvent event) {
        server = event.getServer();
        try {
            relay = ChatRelay.create(
                    BBChatConfig.COMMON.botToken.get(),
                    BBChatConfig.COMMON.guildId.get(),
                    BBChatConfig.COMMON.channelId.get(),
                    BBChatConfig.COMMON.staffRoleId.get(),
                    BBChatConfig.COMMON.commandPrefix.get(),
                    BBChatConfig.COMMON.anyCommands.get().stream().map(String::toString).collect(Collectors.toList()),
                    (msg) -> server.getPlayerList().broadcastMessage(new StringTextComponent(msg), false),
                    () -> new PlayerCountInfo(server.getPlayerCount(), server.getMaxPlayers()),
                    this::handleCommand
            );
        } catch (LoginException e) {
            LOGGER.warn("Failed to login ;-;. Check your bot token.", e);
        }
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
     * @see ServerPlayerEntity#onDeath(DamageSource)
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void relayDeath(LivingDeathEvent event) {
        final LivingEntity living = event.getEntityLiving();
        if (isRealPlayer(living) || (living.hasCustomName() && isRealPlayer(event.getSource().getEntity()))) {
            final World world = living.getCommandSenderWorld();
            if (!world.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES)) return;
            String deathMessage = living.getCombatTracker().getDeathMessage().getString();
            String target = living.getName().getString();
            Entity sourceEntity = event.getSource().getEntity();
            String source = sourceEntity != null ? sourceEntity.getName().getString() : null;
            relay.onDeath(deathMessage, target, source);
        }
    }

    public boolean isRealPlayer(@Nullable Entity entity) {
        if (!(entity instanceof ServerPlayerEntity)) return false;
        ServerPlayerEntity player = (ServerPlayerEntity) entity;
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
        ServerWorld serverWorld = server.getLevel(DimensionType.OVERWORLD);
        CommandSource source = new CommandSource(
                getConsumerSource(response),
                new Vec3d(serverWorld.getSharedSpawnPos()), Vec2f.ZERO, serverWorld, // TODO: Make dynamic
                opLevel,
                name, new StringTextComponent(displayName),
                this.server, null
        );
        server.getCommands().performCommand(source, fullCommand);
    }

    @Nonnull
    private ICommandSource getConsumerSource(Consumer<String> consumer) {
        return new ICommandSource() {
            @Override
            public void sendMessage(ITextComponent component) {
                consumer.accept(component.getColoredString());
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
