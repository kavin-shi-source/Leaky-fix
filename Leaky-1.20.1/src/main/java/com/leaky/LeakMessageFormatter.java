package com.leaky;

public final class LeakMessageFormatter
{
    private LeakMessageFormatter()
    {
    }

    public static String formatBeforePosition(final int itemCount)
    {
        return "发现: " + itemCount + "的物品堆叠量 | 位于 ";
    }

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

    public static String formatFullMessage(final int itemCount, final String position, final String dimensionName, final String playerName,
        final boolean removedItems)
    {
        return formatBeforePosition(itemCount) + position + formatAfterPosition(dimensionName, playerName, removedItems);
    }
}
