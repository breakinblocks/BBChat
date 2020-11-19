package com.breakinblocks.bbchat;

import com.breakinblocks.bbchat.api.ChatRelay;
import com.breakinblocks.bbchat.api.DummyRelay;
import com.breakinblocks.bbchat.api.IRelay;
import com.breakinblocks.bbchat.common.BBChatConfig;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.MinecraftForge;
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
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.util.Objects;
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
    public void relayInit(FMLServerStartingEvent event) {
        server = event.getServer();
        try {
            relay = new ChatRelay(
                    BBChatConfig.COMMON.botToken.get(),
                    BBChatConfig.COMMON.guildId.get(),
                    BBChatConfig.COMMON.channelId.get(),
                    BBChatConfig.COMMON.staffRoleId.get(),
                    BBChatConfig.COMMON.commandPrefix.get(),
                    BBChatConfig.COMMON.anyCommands.get().stream().map(String::toString).collect(Collectors.toList()),
                    (msg) -> server.getPlayerList().sendMessage(new StringTextComponent(msg), false),
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
        String name = event.getPlayer().getName().getFormattedText();
        String text = event.getMessage();
        relay.onChat(name, text);
    }

    @SubscribeEvent
    public void relayLogin(PlayerEvent.PlayerLoggedInEvent event) {
        String name = event.getPlayer().getName().getFormattedText();
        relay.onLogin(name);
    }

    @SubscribeEvent
    public void relayLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        String name = event.getPlayer().getName().getFormattedText();
        relay.onLogout(name);
    }

    @SubscribeEvent
    public void relayAchievement(AdvancementEvent event) {
        Advancement advancement = event.getAdvancement();
        DisplayInfo displayInfo = advancement.getDisplay();
        if (displayInfo == null) return;
        String name = event.getPlayer().getName().getFormattedText();
        String title = displayInfo.getTitle().getFormattedText();
        String description = displayInfo.getDescription().getFormattedText();
        relay.onAchievement(name, title, description);
    }

    /**
     * @see ServerPlayerEntity#onDeath(DamageSource)
     */
    @SubscribeEvent
    public void relayDeath(LivingDeathEvent event) {
        final LivingEntity living = event.getEntityLiving();
        if (living instanceof PlayerEntity || (living.hasCustomName() && event.getSource().getTrueSource() instanceof PlayerEntity)) {
            final World world = living.getEntityWorld();
            if (!world.getGameRules().getBoolean(GameRules.SHOW_DEATH_MESSAGES)) return;
            String deathMessage = living.getCombatTracker().getDeathMessage().getFormattedText();
            String target = living.getName().getFormattedText();
            Entity sourceEntity = event.getSource().getTrueSource();
            String source = sourceEntity != null ? sourceEntity.getName().getFormattedText() : null;
            relay.onDeath(deathMessage, target, source);
        }
    }

    private void handleCommand(boolean isStaff, String name, String displayName, String fullCommand, Consumer<String> response) {
        if (server == null) return;
        // Execute on the main server thread
        if (!server.isOnExecutionThread()) {
            server.execute(() -> handleCommand(isStaff, name, displayName, fullCommand, response));
            return;
        }
        // Create a command source with the correct level
        final int opLevel = isStaff ? server.getOpPermissionLevel() : 0;
        CommandSource source = new CommandSource(
                getConsumerSource(response),
                Vec3d.ZERO, Vec2f.ZERO, server.getWorld(DimensionType.OVERWORLD), // TODO: Make dynamic
                opLevel,
                name, new StringTextComponent(displayName),
                this.server, null
        );
        server.getCommandManager().handleCommand(source, fullCommand);
    }

    @NotNull
    private ICommandSource getConsumerSource(Consumer<String> consumer) {
        return new ICommandSource() {
            @Override
            public void sendMessage(ITextComponent component) {
                final String message = Objects.requireNonNull(TextFormatting.getTextWithoutFormattingCodes(component.getFormattedText()));
                if (message.length() > 0) {
                    consumer.accept(message);
                }
            }

            @Override
            public boolean shouldReceiveFeedback() {
                return true;
            }

            @Override
            public boolean shouldReceiveErrors() {
                return true;
            }

            @Override
            public boolean allowLogging() {
                return true;
            }
        };
    }
}
