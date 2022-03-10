package com.breakinblocks.bbchat.forge;

import com.breakinblocks.bbchat.common.ChatRelay;
import com.breakinblocks.bbchat.common.DummyRelay;
import com.breakinblocks.bbchat.common.IRelay;
import com.breakinblocks.bbchat.common.PlayerCountInfo;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Mod(
        modid = BBChat.MODID,
        name = "BBChat",
        version = "",
        dependencies = "",
        acceptedMinecraftVersions = "",
        acceptableRemoteVersions = "*",
        acceptableSaveVersions = "*"
)
public class BBChat {
    public static final String MODID = "bbchat";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SHOW_DEATH_MESSAGES = "showDeathMessages";
    private IRelay relay = DummyRelay.INSTANCE;
    private MinecraftServer server = null;

    public BBChat() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        BBChatConfig.init(event.getSuggestedConfigurationFile());
    }

    @EventHandler
    public void relayInit(FMLServerAboutToStartEvent event) {
        server = event.getServer();
        try {
            relay = ChatRelay.create(
                    BBChatConfig.botToken,
                    BBChatConfig.guildId,
                    BBChatConfig.channelId,
                    BBChatConfig.staffRoleId,
                    BBChatConfig.commandPrefix,
                    Arrays.stream(BBChatConfig.anyCommands).map(String::toString).collect(Collectors.toList()),
                    (msg) -> server.getPlayerList().sendMessage(new TextComponentString(msg), false),
                    () -> new PlayerCountInfo(server.getCurrentPlayerCount(), server.getMaxPlayers()),
                    this::handleCommand
            );
        } catch (LoginException e) {
            LOGGER.warn("Failed to login ;-;. Check your bot token.", e);
        }
    }

    @EventHandler
    public void relayServerStarted(FMLServerStartedEvent event) {
        relay.onStarted();
    }

    @EventHandler
    public void relayServerStopped(FMLServerStoppedEvent event) {
        relay.onStopped();
        relay.cleanup();
        relay = DummyRelay.INSTANCE;
        server = null;
    }

    @SubscribeEvent
    public void relayChat(ServerChatEvent event) {
        String name = event.getPlayer().getName();
        String text = event.getMessage();
        relay.onChat(name, text);
    }

    @SubscribeEvent
    public void relayLogin(PlayerEvent.PlayerLoggedInEvent event) {
        String name = event.player.getName();
        relay.onLogin(name);
    }

    @SubscribeEvent
    public void relayLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        String name = event.player.getName();
        relay.onLogout(name);
    }

    @SubscribeEvent
    public void relayAchievement(AdvancementEvent event) {
        Advancement advancement = event.getAdvancement();
        DisplayInfo displayInfo = advancement.getDisplay();
        if (displayInfo == null) return;
        String name = event.getEntityPlayer().getName();
        String title = displayInfo.getTitle().getFormattedText();
        String description = displayInfo.getDescription().getFormattedText();
        relay.onAchievement(name, title, description);
    }

    /**
     * @see EntityPlayerMP#onDeath(DamageSource)
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void relayDeath(LivingDeathEvent event) {
        final EntityLivingBase living = event.getEntityLiving();
        if (isRealPlayer(living) || (living.hasCustomName() && isRealPlayer(event.getSource().getTrueSource()))) {
            final World world = living.getEntityWorld();
            if (world.getGameRules().hasRule(SHOW_DEATH_MESSAGES) && !world.getGameRules().getBoolean(SHOW_DEATH_MESSAGES))
                return;
            String deathMessage = living.getCombatTracker().getDeathMessage().getFormattedText();
            String target = living.getName();
            Entity sourceEntity = event.getSource().getTrueSource();
            String source = sourceEntity != null ? sourceEntity.getName() : null;
            relay.onDeath(deathMessage, target, source);
        }
    }

    public boolean isRealPlayer(@Nullable Entity entity) {
        if (!(entity instanceof EntityPlayerMP)) return false;
        EntityPlayerMP player = (EntityPlayerMP) entity;
        if (player instanceof FakePlayer) return false;
        return player.connection != null;
    }

    private void handleCommand(boolean isStaff, String name, String displayName, String fullCommand, Consumer<String> response) {
        if (server == null) return;
        // Execute on the main server thread
        if (!server.isCallingFromMinecraftThread()) {
            server.callFromMainThread(Executors.callable(() -> handleCommand(isStaff, name, displayName, fullCommand, response)));
            return;
        }
        // Create a command source with the correct level
        final int opLevel = isStaff ? server.getOpPermissionLevel() : 0;
        ICommandSender sender = new ICommandSender() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public ITextComponent getDisplayName() {
                return new TextComponentString(displayName);
            }

            @Override
            public void sendMessage(ITextComponent component) {
                response.accept(component.getFormattedText());
            }

            @Override
            public boolean canUseCommand(int permLevel, String commandName) {
                return opLevel >= permLevel;
            }

            @Override
            public BlockPos getPosition() {
                return BlockPos.ORIGIN; // TODO: Make dynamic
            }

            @Override
            public Vec3d getPositionVector() {
                return Vec3d.ZERO; // TODO: Make dynamic
            }

            @Override
            public World getEntityWorld() {
                return server.getEntityWorld();
            }

            @Override
            public boolean sendCommandFeedback() {
                return true;
            }

            @Nullable
            @Override
            public MinecraftServer getServer() {
                return server;
            }
        };
        server.getCommandManager().executeCommand(sender, fullCommand);
    }
}
