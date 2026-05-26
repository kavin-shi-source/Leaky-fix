package com.leaky;

import com.cupboard.config.CupboardConfig;
import com.leaky.config.CommonConfiguration;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod(com.leaky.Leaky.MODID)
public class Leaky
{
    public static final String                              MODID              = "leaky";
    public static final Logger                              LOGGER             = LogManager.getLogger();
    public static       CupboardConfig<CommonConfiguration> config             = new CupboardConfig<>(MODID, new CommonConfiguration());
    public static final int                                 CONTAIN_RADIUS_SQR = 4 * 4;
    public static final int                                 REPORT_RADIUS_SQR  = 10 * 10;

    private static Object2LongOpenHashMap<String> reportedLocations = new Object2LongOpenHashMap<>();

    private static String dimPosKey(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, BlockPos pos)
    {
        return dimension.location().toString() + "@" + pos.toShortString();
    }

    public Leaky()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event)
    {
        config.load();
        LOGGER.info("{} mod initialized", MODID);
    }

    public static void detectedItemLeak(final ItemEntity entity, final List<ItemEntity> items, final int range)
    {
        final CommonConfiguration cfg = config.getCommonConfig();
        int size = items.size();
        for (final ItemEntity item : items)
        {
            if (item instanceof INearbyItemAwareEntity nearbyItemAware)
            {
                nearbyItemAware.setNearbyItems(size);
            }
        }

        int effectiveSize = size;
        if (range > 2 && size < cfg.autoRemoveThreshold * 3)
        {
            effectiveSize /= 2;
        }

        if (effectiveSize < cfg.reportThreshold)
        {
            return;
        }

        boolean contained = false;
        boolean cooldownHit = false;

        long now = entity.level().getGameTime();
        long expiryTime = (long) cfg.reportInterval * 20L * 2;
        List<String> expired = new ArrayList<>();
        for (final Map.Entry<String, Long> entry : reportedLocations.entrySet())
        {
            String key = entry.getKey();
            String dimPart = key.substring(0, key.indexOf('@'));
            if (!dimPart.equals(entity.level().dimension().location().toString()))
            {
                if ((now - entry.getValue()) > expiryTime)
                {
                    expired.add(key);
                }
                continue;
            }

            String posPart = key.substring(key.indexOf('@') + 1);
            String[] coords = posPart.split(", ");
            int bx = Integer.parseInt(coords[0]);
            int by = Integer.parseInt(coords[1]);
            int bz = Integer.parseInt(coords[2]);
            double dist = new BlockPos(bx, by, bz).distSqr(entity.blockPosition());

            if (dist < CONTAIN_RADIUS_SQR)
            {
                contained = true;
            }

            if (dist < REPORT_RADIUS_SQR && (now - entry.getValue()) < cfg.reportInterval * 20L)
            {
                cooldownHit = true;
            }

            if ((now - entry.getValue()) > expiryTime)
            {
                expired.add(key);
            }
        }
        expired.forEach(reportedLocations::removeLong);

        if (cooldownHit)
        {
            return;
        }

        reportedLocations.put(dimPosKey(entity.level().dimension(), entity.blockPosition()), entity.level().getGameTime());

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

        String nearestPlayerName = nearestPlayer != null ? nearestPlayer.getName().getString() : "无";

        boolean removedItems = size > cfg.autoRemoveThreshold && (contained || size >= cfg.autoRemoveThreshold * 3);

        String tpCommand = "/execute in " + entity.level().dimension().location() + " run tp " + entity.getBlockX() + " " + entity.getBlockY() + " " + entity.getBlockZ();
        MutableComponent component = LeakMessageFormatter.buildComponent(
            items.size(),
            entity.blockPosition().toShortString(),
            entity.level().dimension().location().toString(),
            entity.level().dimension().location().toLanguageKey(),
            nearestPlayerName,
            tpCommand,
            removedItems
        );

        if (removedItems)
        {
            items.forEach(Entity::discard);
        }

        if (cfg.chatNotification.equalsIgnoreCase("PLAYER"))
        {
            if (nearestPlayer != null)
            {
                nearestPlayer.sendSystemMessage(component);
            }
        }
        else if (cfg.chatNotification.equalsIgnoreCase("EVERYONE"))
        {
            if (entity.level().getServer() != null)
            {
                for (final Player player : entity.level().getServer().getPlayerList().getPlayers())
                {
                    player.sendSystemMessage(component);
                }
            }
        }
        else if (cfg.chatNotification.equalsIgnoreCase("OP"))
        {
            if (entity.level().getServer() != null)
            {
                double dist = Double.MAX_VALUE;
                Player closestOp = null;
                for (final Player player : entity.level().players())
                {
                    if (entity.level().getServer().getPlayerList().isOp(player.getGameProfile()) && player.position().distanceTo(entity.position()) < dist)
                    {
                        dist = player.position().distanceTo(entity.position());
                        closestOp = player;
                    }
                }

                if (closestOp != null)
                {
                    closestOp.sendSystemMessage(component);
                }
            }
        }

        Leaky.LOGGER.warn(component.getString());
    }
}
