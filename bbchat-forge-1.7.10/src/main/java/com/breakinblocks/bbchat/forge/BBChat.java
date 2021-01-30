package com.breakinblocks.bbchat.forge;

import com.breakinblocks.bbchat.common.ChatRelay;
import com.breakinblocks.bbchat.common.DummyRelay;
import com.breakinblocks.bbchat.common.IRelay;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.Achievement;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AchievementEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
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
    private IRelay relay = DummyRelay.INSTANCE;
    private MinecraftServer server = null;
    private Thread serverThread = null;
    private LinkedList<Callable<Object>> taskQueue = new LinkedList<>();

    public BBChat() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        BBChatConfig.init(event.getSuggestedConfigurationFile());
    }

    @EventHandler
    public void relayInit(FMLServerStartingEvent event) {
        server = event.getServer();
        serverThread = Thread.currentThread();
        try {
            //noinspection unchecked
            relay = ChatRelay.create(
                    BBChatConfig.botToken,
                    BBChatConfig.guildId,
                    BBChatConfig.channelId,
                    BBChatConfig.staffRoleId,
                    BBChatConfig.commandPrefix,
                    Arrays.stream(BBChatConfig.anyCommands).map(String::toString).collect(Collectors.toList()),
                    msg -> ((List<EntityPlayerMP>) server.getConfigurationManager().playerEntityList).forEach(player -> player.addChatMessage(new ChatComponentText(msg))),
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
        String name = event.player.getDisplayName();
        String text = event.message;
        relay.onChat(name, text);
    }

    @SubscribeEvent
    public void relayLogin(PlayerEvent.PlayerLoggedInEvent event) {
        String name = event.player.getDisplayName();
        relay.onLogin(name);
    }

    @SubscribeEvent
    public void relayLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        String name = event.player.getDisplayName();
        relay.onLogout(name);
    }

    @SubscribeEvent
    public void relayAchievement(AchievementEvent event) {
        Achievement achievement = event.achievement;
        achievement.isAchievement();
        if (!achievement.isAchievement()) return;
        String name = event.entityPlayer.getDisplayName();
        String title = achievement.func_150951_e().getFormattedText();
        String description = achievement.getDescription();
        relay.onAchievement(name, title, description);
    }

    /**
     * @see EntityPlayerMP#onDeath(DamageSource)
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void relayDeath(LivingDeathEvent event) {
        final EntityLivingBase living = event.entityLiving;
        final World world = living.worldObj;
        if (isRealPlayer(living) || (living instanceof EntityLiving && ((EntityLiving) living).hasCustomNameTag() && isRealPlayer(event.source.getEntity()))) {
            if (!world.getGameRules().getGameRuleBooleanValue("showDeathMessages")) return;
            String deathMessage = living.func_110142_aN().func_151521_b().getFormattedText();
            String target = getName(living);
            Entity sourceEntity = event.source.getSourceOfDamage();
            String source = sourceEntity != null ? getName(sourceEntity) : null;
            relay.onDeath(deathMessage, target, source);
        }
    }

    public boolean isRealPlayer(@Nullable Entity entity) {
        if (!(entity instanceof EntityPlayerMP)) return false;
        EntityPlayerMP player = (EntityPlayerMP) entity;
        if (player instanceof FakePlayer) return false;
        return player.playerNetServerHandler != null;
    }

    public String getName(Entity entity) {
        if (entity instanceof EntityPlayer) {
            return ((EntityPlayer) entity).getDisplayName();
        }
        if (entity instanceof EntityLiving) {
            EntityLiving living = (EntityLiving) entity;
            if (living.hasCustomNameTag())
                return living.getCustomNameTag();
        }
        return entity.getCommandSenderName();
    }

    private void handleCommand(boolean isStaff, String name, String displayName, String fullCommand, Consumer<String> response) {
        if (server == null) return;
        // Execute on the main server thread
        if (!isCallingFromMinecraftThread()) {
            callFromMainThread(Executors.callable(() -> handleCommand(isStaff, name, displayName, fullCommand, response)));
            return;
        }
        // Create a command source with the correct level
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
                response.accept(component.getFormattedText());
            }

            @Override
            public boolean canCommandSenderUseCommand(int permLevel, String idk) {
                return opLevel >= permLevel;
            }

            @Override
            public ChunkCoordinates getPlayerCoordinates() {
                return new ChunkCoordinates(0, 0, 0); // TODO: Make dynamic
            }

            @Override
            public World getEntityWorld() {
                return server.getEntityWorld();
            }
        };
        server.getCommandManager().executeCommand(sender, fullCommand);
    }

    private void callFromMainThread(Callable<Object> callable) {
        taskQueue.offer(callable);
    }

    private boolean isCallingFromMinecraftThread() {
        return Thread.currentThread() == serverThread;
    }

    @SubscribeEvent
    public void runTasks(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            while (!taskQueue.isEmpty()) {
                try {
                    taskQueue.poll().call();
                } catch (Exception e) {
                    LOGGER.error("Error while processing command / task", e);
                }
            }
        }
    }
}
