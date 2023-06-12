package com.breakinblocks.bbchat.forge;

import com.breakinblocks.bbchat.core.impl.ChatRelay;
import com.breakinblocks.bbchat.core.impl.DummyRelay;
import com.breakinblocks.bbchat.core.Relay;
import com.breakinblocks.bbchat.core.PlayerCountInfo;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
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
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatisticsFile;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AchievementEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.LinkedList;
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
    private static final String SHOW_DEATH_MESSAGES = "showDeathMessages";
    private static final Logger LOGGER = LogManager.getLogger();
    private Relay relay = DummyRelay.INSTANCE;
    private MinecraftServer server = null;
    private Thread serverThread = null;
    private LinkedList<Callable<Object>> taskQueue = new LinkedList<>();

    public BBChat() {
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        BBChatConfig.init(event.getSuggestedConfigurationFile());
    }

    @EventHandler
    public void relayInit(FMLServerAboutToStartEvent event) {
        server = event.getServer();
        serverThread = Thread.currentThread();
        relay = ChatRelay.create(
                BBChatConfig.botToken,
                BBChatConfig.guildId,
                BBChatConfig.channelId,
                BBChatConfig.staffRoleId,
                BBChatConfig.commandPrefix,
                Arrays.stream(BBChatConfig.anyCommands).map(String::toString).collect(Collectors.toList()),
                msg -> server.getConfigurationManager().sendChatMsgImpl(new ChatComponentText(msg), false),
                () -> new PlayerCountInfo(server.getCurrentPlayerCount(), server.getMaxPlayers()),
                this::handleCommand
        );
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

    /**
     * Server side version of
     *
     * @see ChatStyle#getFormattingCode();
     */
    private String getFormattingCode(ChatStyle style) {
        if (style.isEmpty()) {
            return "";
        } else {
            StringBuilder stringbuilder = new StringBuilder();

            if (style.getColor() != null) {
                stringbuilder.append(style.getColor());
            }

            if (style.getBold()) {
                stringbuilder.append(EnumChatFormatting.BOLD);
            }

            if (style.getItalic()) {
                stringbuilder.append(EnumChatFormatting.ITALIC);
            }

            if (style.getUnderlined()) {
                stringbuilder.append(EnumChatFormatting.UNDERLINE);
            }

            if (style.getObfuscated()) {
                stringbuilder.append(EnumChatFormatting.OBFUSCATED);
            }

            if (style.getStrikethrough()) {
                stringbuilder.append(EnumChatFormatting.STRIKETHROUGH);
            }

            return stringbuilder.toString();
        }
    }

    /**
     * Server side version of
     *
     * @see IChatComponent#getFormattedText()
     */
    private String getFormattedText(IChatComponent component) {
        StringBuilder stringbuilder = new StringBuilder();

        for (Object o : component) {
            IChatComponent ichatcomponent = (IChatComponent) o;
            stringbuilder.append(getFormattingCode(ichatcomponent.getChatStyle()));
            stringbuilder.append(ichatcomponent.getUnformattedTextForChat());
            stringbuilder.append(EnumChatFormatting.RESET);
        }

        return stringbuilder.toString();
    }

    /**
     * Server side version of
     *
     * @see Achievement#getDescription()
     */
    private String getDescription(Achievement achievement) {
        String translated = StatCollector.translateToLocal(achievement.achievementDescription);
        try {
            if (achievement.statId.equals("achievement.openInventory")) {
                return String.format(translated, 'E');
            }
        } catch (IllegalFormatException ignored) {
        }
        return translated;
    }

    /**
     * @see StatisticsFile#func_150873_a(EntityPlayer, StatBase, int)
     */
    @SubscribeEvent
    public void relayAchievement(AchievementEvent event) {
        Achievement achievement = event.achievement;
        StatisticsFile statisticsFile = server.getConfigurationManager().func_152602_a(event.entityPlayer);
        boolean hasRequirements = statisticsFile.canUnlockAchievement(achievement);
        boolean alreadyObtained = statisticsFile.hasAchievementUnlocked(achievement);
        if (hasRequirements && !alreadyObtained) {
            String name = event.entityPlayer.getDisplayName();
            String title = getFormattedText(achievement.func_150951_e());
            String description = getDescription(achievement);
            relay.onAchievement(name, title, description);
        }
    }

    /**
     * @see EntityPlayerMP#onDeath(DamageSource)
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void relayDeath(LivingDeathEvent event) {
        final EntityLivingBase living = event.entityLiving;
        final World world = living.worldObj;
        if (isRealPlayer(living) || (living instanceof EntityLiving && ((EntityLiving) living).hasCustomNameTag() && isRealPlayer(event.source.getEntity()))) {
            if (world.getGameRules().hasRule(SHOW_DEATH_MESSAGES) && !world.getGameRules().getGameRuleBooleanValue(SHOW_DEATH_MESSAGES))
                return;
            String deathMessage = getFormattedText(living.func_110142_aN().func_151521_b());
            String target = getName(living);
            Entity sourceEntity = event.source.getSourceOfDamage();
            String source = sourceEntity != null ? getName(sourceEntity) : null;
            relay.onDeath(deathMessage, target, source);
        }
    }

    private boolean isRealPlayer(@Nullable Entity entity) {
        if (!(entity instanceof EntityPlayerMP)) return false;
        EntityPlayerMP player = (EntityPlayerMP) entity;
        if (player instanceof FakePlayer) return false;
        return player.playerNetServerHandler != null;
    }

    private String getName(Entity entity) {
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
                response.accept(getFormattedText(component));
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
