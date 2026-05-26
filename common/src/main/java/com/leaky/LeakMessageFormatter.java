package com.leaky;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class LeakMessageFormatter
{
    private LeakMessageFormatter()
    {
    }

    @Deprecated
    public static String formatBeforePosition(final int itemCount)
    {
        return "发现: " + itemCount + "的物品堆叠量 | 位于 ";
    }

    @Deprecated
    public static String formatAfterPosition(final String dimensionName, final String playerName, final boolean removedItems)
    {
        final StringBuilder builder = new StringBuilder()
            .append(" | 维度: ")
            .append(dimensionName)
            .append(" | 最近的玩家ID为 ")
            .append(playerName);

        if (removedItems)
        {
            builder.append(". 已自动清理物品以防止卡顿");
        }

        return builder.toString();
    }

    @Deprecated
    public static String formatFullMessage(final int itemCount, final String position, final String dimensionName, final String playerName,
        final boolean removedItems)
    {
        return formatBeforePosition(itemCount) + position + formatAfterPosition(dimensionName, playerName, removedItems);
    }

    public static MutableComponent buildComponent(final int itemCount, final String positionShort,
        final String dimensionLocation, final String dimensionTranslationKey,
        final String playerName, final String tpCommand, final boolean removedItems)
    {
        MutableComponent component = Component.translatable("leaky.detect", itemCount)
            .withStyle(ChatFormatting.RED)
            .append(Component.literal("[" + positionShort + "]")
                .withStyle(ChatFormatting.YELLOW)
                .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))))
            .append(Component.translatable("leaky.dimension", Component.translatable(dimensionTranslationKey)))
            .append(Component.translatable("leaky.nearest_player", playerName));

        if (removedItems)
        {
            component.append(Component.translatable("leaky.removedItems"));
        }

        return component;
    }
}
