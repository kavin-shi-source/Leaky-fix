package com.leaky;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeakMessageFormatterTest
{
    @Test
    void formatsFullMessageForServerOnlyClients()
    {
        final String message = LeakMessageFormatter.formatFullMessage(219, "[-235, 88, 1016]", "overworld", "kavinShi157", false);

        assertEquals("发现: 219的物品堆叠量 | 位于 [-235, 88, 1016] | 维度: overworld | 最近的玩家ID为 kavinShi157", message);
    }

    @Test
    void appendsRemovalNoticeWhenItemsWereDiscarded()
    {
        final String message = LeakMessageFormatter.formatFullMessage(500, "[-235, 88, 1016]", "overworld", "kavinShi157", true);

        assertEquals("发现: 500的物品堆叠量 | 位于 [-235, 88, 1016] | 维度: overworld | 最近的玩家ID为 kavinShi157. 已自动清理物品以防止卡顿", message);
    }
}
