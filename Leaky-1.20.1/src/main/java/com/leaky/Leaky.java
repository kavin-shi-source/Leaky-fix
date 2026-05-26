package com.leaky;

import com.cupboard.config.CupboardConfig;
import com.leaky.config.CommonConfiguration;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Random;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(com.leaky.Leaky.MODID)
public class Leaky
{
    public static final String                              MODID              = "leaky";
    public static final Logger                              LOGGER             = LogManager.getLogger();
    public static       CupboardConfig<CommonConfiguration> config             = new CupboardConfig<>(MODID, new CommonConfiguration());
    public static final int                                 CONTAIN_RADIUS_SQR = 4 * 4;
    public static final int                                 REPORT_RADIUS_SQR  = 10 * 10;
    public static       Random                              rand               = new Random();

    private static final long CLEANUP_INTERVAL = 20 * 60 * 10;
    private static final long EXPIRY_TIME_MULTIPLIER = 10;

    private static Object2LongOpenHashMap<BlockPos> reportedLocations = new Object2LongOpenHashMap<>();
    private static long lastCleanupTime = 0;

    public Leaky()
    {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        config.load();
        LOGGER.info(MODID + " mod initialized");
    }

    public static void detectedItemLeak(final ItemEntity entity, final List<ItemEntity> items, final int range)
    {
        int size = items.size();
        for (final ItemEntity item : items)
        {
            if (item instanceof INearbyItemAwareEntity nearbyItemAware)
            {
                nearbyItemAware.setNearbyItems(size);
            }
        }

        if (range > 2 && size < config.getCommonConfig().autoremovethreshold * 3)
        {
            size /= 2;
        }

        if (size < Leaky.config.getCommonConfig().reportThreshold)
        {
            return;
        }

        boolean contained = false;

        // Large leaks bypass the cooldown, to allow deletion before server crashes
        if (size < config.getCommonConfig().autoremovethreshold * 3)
        {
            long now = entity.level().getGameTime();
            for (final Map.Entry<BlockPos, Long> entry : reportedLocations.entrySet())
            {
                final double dist = entry.getKey().distSqr(entity.blockPosition());

                if (dist < CONTAIN_RADIUS_SQR)
                {
                    contained = true;
                }

                if (dist < REPORT_RADIUS_SQR && (now - entry.getValue()) < config.getCommonConfig().reportInterval * 20)
                {
                    return;
                }
            }
        }

        long now = entity.level().getGameTime();
        reportedLocations.put(entity.blockPosition(), now);

        if (now - lastCleanupTime > CLEANUP_INTERVAL)
        {
            lastCleanupTime = now;
            long expiryTime = config.getCommonConfig().reportInterval * 20L * EXPIRY_TIME_MULTIPLIER;
            reportedLocations.object2LongEntrySet().removeIf(entry -> (now - entry.getLongValue()) > expiryTime);
        }

        Player nearestPlayer = null;
        double nearestDist = Double.MAX_VALUE;
        for (final Player player : entity.level().players())
        {
            if (player.position().distanceTo(entity.position()) < nearestDist)
            {
                nearestDist = player.position().distanceTo(entity.position());
                nearestPlayer = player;
            }
        }

        String playerName = nearestPlayer != null ? nearestPlayer.getName().getString() : "无";
        String dimensionName = entity.level().dimension().location().getPath();

        final boolean removedItems = size > config.getCommonConfig().autoremovethreshold
            && (contained || size >= config.getCommonConfig().autoremovethreshold * 3);

        MutableComponent component = Component.literal(LeakMessageFormatter.formatBeforePosition(items.size()))
            .withStyle(ChatFormatting.LIGHT_PURPLE)
            .append(Component.literal("[" + entity.blockPosition().toShortString() + "]")
                .withStyle(ChatFormatting.YELLOW).withStyle(style ->
                {
                    return style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/tp " + entity.getBlockX() + " " + entity.getBlockY() + " " + entity.getBlockZ()));
                }))
            .append(Component.literal(LeakMessageFormatter.formatAfterPosition(dimensionName, playerName, removedItems))
                .withStyle(ChatFormatting.LIGHT_PURPLE));

        if (removedItems)
        {
            items.forEach(Entity::discard);
        }

        if (config.getCommonConfig().chatnotification.equalsIgnoreCase("PLAYER"))
        {
            if (nearestPlayer != null)
            {
                for (int i = 0; i < 5; i++)
                {
                    nearestPlayer.sendSystemMessage(component);
                }
            }
        }
        else if (config.getCommonConfig().chatnotification.equalsIgnoreCase("EVERYONE"))
        {
            if (entity.level().getServer() != null)
            {
                for (final Player player : entity.level().getServer().getPlayerList().getPlayers())
                {
                    for (int i = 0; i < 5; i++)
                    {
                        player.sendSystemMessage(component);
                    }
                }
            }
        }
        else if (config.getCommonConfig().chatnotification.equalsIgnoreCase("OP"))
        {
            Player nearestOp = null;
            double opNearestDist = Double.MAX_VALUE;
            for (final Player player : entity.level().players())
            {
                if (player.level().getServer() != null 
                    && player.level().getServer().getPlayerList().isOp(player.getGameProfile()) 
                    && player.position().distanceTo(entity.position()) < opNearestDist)
                {
                    opNearestDist = player.position().distanceTo(entity.position());
                    nearestOp = player;
                }
            }

            if (nearestOp != null)
            {
                for (int i = 0; i < 5; i++)
                {
                    nearestOp.sendSystemMessage(component);
                }
            }
        }

        Leaky.LOGGER.warn(component.getString());
    }
}
