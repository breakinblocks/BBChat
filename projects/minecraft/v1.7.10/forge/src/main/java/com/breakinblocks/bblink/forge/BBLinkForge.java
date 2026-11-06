package com.breakinblocks.bblink.forge;

import com.breakinblocks.bblink.core.api.NameSanitizer;
import com.breakinblocks.bblink.core.api.RelayService;
import com.breakinblocks.bblink.forge.common.ForgeMinecraftService;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.Achievement;
import net.minecraft.stats.StatisticsFile;
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
import java.util.IllegalFormatException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

@Mod(
        modid = BBLinkForge.MOD_ID,
        name = "BBLink",
        version = Tags.VERSION,
        dependencies = "required-after:Forge@[10.13.4.1614,)",
        acceptedMinecraftVersions = "[1.7.10]",
        // Server side only: clients don't need it installed.
        acceptableRemoteVersions = "*"
)
public class BBLinkForge {
    public static final String MOD_ID = "bblink";
    private static final Logger LOGGER = LogManager.getLogger("BBLink");
    private static final String SHOW_DEATH_MESSAGES = "showDeathMessages";
    private static final Queue<Consumer<MinecraftServer>> SERVER_TASKS = new ConcurrentLinkedQueue<>();
    @Nullable
    private static volatile MinecraftServer server;

    public BBLinkForge() {
        RelayService.INSTANCE.setMinecraftService(new ForgeMinecraftService());
    }

    @Nullable
    public static MinecraftServer getServer() {
        return server;
    }

    /**
     * Run a task on the server thread at the start of the next tick. Dropped if the server stops first.
     */
    public static void runOnServerThread(Consumer<MinecraftServer> task) {
        if (server == null) return;
        SERVER_TASKS.add(task);
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @EventHandler
    public void serverAboutToStart(FMLServerAboutToStartEvent event) {
        server = event.getServer();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        RelayService.INSTANCE.onStarting();
    }

    @EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        RelayService.INSTANCE.onStarted();
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        RelayService.INSTANCE.onStopping();
    }

    @EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        RelayService.INSTANCE.onStopped();
        server = null;
        SERVER_TASKS.clear();
    }

    @SubscribeEvent
    public void runServerTasks(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        MinecraftServer current = server;
        if (current == null) return;
        for (Consumer<MinecraftServer> task = SERVER_TASKS.poll(); task != null; task = SERVER_TASKS.poll()) {
            try {
                task.accept(current);
            } catch (RuntimeException e) {
                LOGGER.error("Error while running a relayed message or command", e);
            }
        }
    }

    /**
     * Lowest priority so chat formatting / filtering mods run first; cancelled messages are not relayed.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void relayChat(ServerChatEvent event) {
        RelayService.INSTANCE.onChat(getPlayerName(event.player), event.message);
    }

    @SubscribeEvent
    public void relayLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!isRealPlayer(event.player)) return;
        RelayService.INSTANCE.onLogin(getPlayerName(event.player));
    }

    @SubscribeEvent
    public void relayLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!isRealPlayer(event.player)) return;
        RelayService.INSTANCE.onLogout(getPlayerName(event.player));
    }

    /**
     * Only announce achievements the player is actually unlocking now (the event also fires for ones they already
     * have or can't get yet).
     *
     * @see StatisticsFile#func_150873_a
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void relayAchievement(AchievementEvent event) {
        MinecraftServer current = server;
        if (current == null || !isRealPlayer(event.entityPlayer)) return;
        Achievement achievement = event.achievement;
        StatisticsFile stats = current.getConfigurationManager().func_152602_a(event.entityPlayer);
        if (!stats.canUnlockAchievement(achievement) || stats.hasAchievementUnlocked(achievement)) return;
        String title = achievement.func_150951_e().getUnformattedText();
        RelayService.INSTANCE.onAchievement(getPlayerName(event.entityPlayer), title, getDescription(achievement));
    }

    /**
     * @see EntityPlayerMP#onDeath
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void relayDeath(LivingDeathEvent event) {
        EntityLivingBase living = event.entityLiving;
        if (!isRealPlayer(living)) return;
        World world = living.worldObj;
        if (world.getGameRules().hasRule(SHOW_DEATH_MESSAGES) && !world.getGameRules().getGameRuleBooleanValue(SHOW_DEATH_MESSAGES))
            return;
        EntityPlayer player = (EntityPlayer) living;
        String deathMessage = living.func_110142_aN().func_151521_b().getUnformattedText();
        // The death message is built from the team-formatted display name; swap in the clean name.
        String target = getPlayerName(player);
        deathMessage = replaceFirst(deathMessage, player.func_145748_c_().getUnformattedText(), target);
        // getEntity() is the attacker (e.g. the skeleton), not the projectile, matching the death message.
        Entity sourceEntity = event.source.getEntity();
        String source = null;
        if (sourceEntity instanceof EntityPlayer) {
            EntityPlayer sourcePlayer = (EntityPlayer) sourceEntity;
            source = getPlayerName(sourcePlayer);
            deathMessage = replaceFirst(deathMessage, sourcePlayer.func_145748_c_().getUnformattedText(), source);
        } else if (sourceEntity != null) {
            source = NameSanitizer.stripFormatting(sourceEntity.func_145748_c_().getUnformattedText());
        }
        RelayService.INSTANCE.onDeath(deathMessage, target, source);
    }

    /**
     * Nickname if one is set (Forge's {@code PlayerEvent.NameFormat}, used by nickname mods), otherwise the
     * account name. In 1.7.10 the display name doesn't include team prefixes; colour codes, gradients and
     * decorative characters are removed by {@link NameSanitizer}.
     */
    private static String getPlayerName(EntityPlayer player) {
        return NameSanitizer.cleanName(player.getDisplayName(), player.getCommandSenderName());
    }

    private static boolean isRealPlayer(@Nullable Entity entity) {
        if (!(entity instanceof EntityPlayerMP)) return false;
        if (entity instanceof FakePlayer) return false;
        return ((EntityPlayerMP) entity).playerNetServerHandler != null;
    }

    /**
     * Server side version of {@code Achievement#getDescription()} (client only).
     */
    private static String getDescription(Achievement achievement) {
        String key;
        try {
            key = ReflectionHelper.getPrivateValue(Achievement.class, achievement, "achievementDescription", "field_75996_k");
        } catch (RuntimeException e) {
            return "";
        }
        String translated = StatCollector.translateToLocal(key);
        try {
            if ("achievement.openInventory".equals(achievement.statId)) {
                return String.format(translated, 'E');
            }
        } catch (IllegalFormatException ignored) {
        }
        return translated;
    }

    private static String replaceFirst(String text, String search, String replacement) {
        if (search.isEmpty() || search.equals(replacement)) return text;
        int index = text.indexOf(search);
        if (index < 0) return text;
        return text.substring(0, index) + replacement + text.substring(index + search.length());
    }
}
