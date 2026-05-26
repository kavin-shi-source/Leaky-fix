package com.leaky.config;

import com.cupboard.config.ICommonConfig;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.function.Function;

public class CommonConfiguration implements ICommonConfig
{
    public int     reportInterval       = 60 * 3;
    public String  chatNotification     = "PLAYER";
    public boolean highlightItems       = true;
    public int     reportThreshold      = 200;
    public int     autoRemoveThreshold  = 400;
    public boolean improveItemPerformance = true;

    public CommonConfiguration()
    {
    }

    public JsonObject serialize()
    {
        final JsonObject root = new JsonObject();

        final JsonObject entry8 = new JsonObject();
        entry8.addProperty("desc:", "优化物品实体性能，显著降低其开销。默认值: true");
        entry8.addProperty("improveItemPerformance", improveItemPerformance);
        root.add("improveItemPerformance", entry8);

        final JsonObject entry5 = new JsonObject();
        entry5.addProperty("desc:", "是否高亮显示附近堆叠的物品实体。默认值: true");
        entry5.addProperty("highlightItems", highlightItems);
        root.add("highlightItems", entry5);

        final JsonObject entry6 = new JsonObject();
        entry6.addProperty("desc:", "触发报告的最小物品堆叠数量。默认值: 200");
        entry6.addProperty("reportThreshold", reportThreshold);
        root.add("reportThreshold", entry6);

        final JsonObject entry7 = new JsonObject();
        entry7.addProperty("desc:", "自动清理的物品堆叠数量阈值，首次检测后等待报告间隔时间才会清理。如果堆叠数量超过阈值的3倍则立即清理。默认值: 400");
        entry7.addProperty("autoRemoveThreshold", autoRemoveThreshold);
        root.add("autoRemoveThreshold", entry7);

        final JsonObject entry3 = new JsonObject();
        entry3.addProperty("desc:", "重复通知的间隔时间（秒）。默认值: 180");
        entry3.addProperty("reportInterval", reportInterval);
        root.add("reportInterval", entry3);

        final JsonObject entry4 = new JsonObject();
        entry4.addProperty("desc:", "聊天通知类型: PLAYER(最近的玩家), EVERYONE(所有玩家), NONE(无), OP(管理员)。默认值: PLAYER");
        entry4.addProperty("chatNotification", chatNotification);
        root.add("chatNotification", entry4);

        return root;
    }

    public void deserialize(JsonObject data)
    {
        reportInterval = getSafeValue(data, "reportInterval", reportInterval, JsonElement::getAsInt);
        chatNotification = getSafeValue(data, "chatNotification", chatNotification, JsonElement::getAsString);
        highlightItems = getSafeValue(data, "highlightItems", highlightItems, JsonElement::getAsBoolean);
        improveItemPerformance = getSafeValue(data, "improveItemPerformance", improveItemPerformance, JsonElement::getAsBoolean);
        reportThreshold = getSafeValue(data, "reportThreshold", reportThreshold, JsonElement::getAsInt);
        autoRemoveThreshold = getSafeValue(data, "autoRemoveThreshold", autoRemoveThreshold, JsonElement::getAsInt);

        validate();
    }

    private void validate()
    {
        if (reportInterval < 1)
        {
            reportInterval = 60 * 3;
        }
        if (reportThreshold < 1)
        {
            reportThreshold = 200;
        }
        if (autoRemoveThreshold < reportThreshold)
        {
            autoRemoveThreshold = 400;
        }
        if (!chatNotification.equalsIgnoreCase("PLAYER") && !chatNotification.equalsIgnoreCase("EVERYONE")
            && !chatNotification.equalsIgnoreCase("OP") && !chatNotification.equalsIgnoreCase("NONE"))
        {
            chatNotification = "PLAYER";
        }
    }

    private <T> T getSafeValue(JsonObject data, String key, T defaultValue, Function<JsonElement, T> parser)
    {
        try
        {
            if (data.has(key) && data.get(key).isJsonObject())
            {
                JsonObject obj = data.getAsJsonObject(key);
                if (obj.has(key))
                {
                    return parser.apply(obj.get(key));
                }
            }
        }
        catch (Exception ignored)
        {
        }
        return defaultValue;
    }
}
