package com.leaky.config;

import com.cupboard.config.ICommonConfig;
import com.google.gson.JsonObject;
import com.leaky.Leaky;

public class CommonConfiguration implements ICommonConfig
{
    public int     reportInterval      = 60 * 3;
    public String  chatnotification    = "PLAYER";
    public boolean highlightitems      = true;
    public int     reportThreshold     = 200;
    public int     autoremovethreshold = 400;
    public boolean improveItemPerformance = true;

    public CommonConfiguration()
    {

    }

    public JsonObject serialize()
    {
        final JsonObject root = new JsonObject();

        final JsonObject entry8 = new JsonObject();
        entry8.addProperty("desc:", "Improves item entity performance, significantly reducing their overhead. default: true");
        entry8.addProperty("improveItemPerformance", improveItemPerformance);
        root.add("improveItemPerformance", entry8);

        final JsonObject entry5 = new JsonObject();
        entry5.addProperty("desc:", "Choose if nearby reported stacked item entities should be glowing, default: true");
        entry5.addProperty("highlightitems", highlightitems);
        root.add("highlightitems", entry5);

        final JsonObject entry6 = new JsonObject();
        entry6.addProperty("desc:", "Set the min amount of stacked items being reported, default: 200");
        entry6.addProperty("reportThreshold", reportThreshold);
        root.add("reportThreshold", entry6);

        final JsonObject entry7 = new JsonObject();
        entry7.addProperty("desc:", "Set the amount of stacked items being automatically removed, stacks of items only get removed after waiting the reportInterval after first detection. If the stacks exceed the autoremoval threshold by x3 they are instantly removed. default: 400");
        entry7.addProperty("autoremovethreshold", autoremovethreshold);
        root.add("autoremovethreshold", entry7);

        final JsonObject entry3 = new JsonObject();
        entry3.addProperty("desc:", "Set the amount of seconds between repeated notifications, default: 180");
        entry3.addProperty("reportInterval", reportInterval);
        root.add("reportInterval", entry3);

        final JsonObject entry4 = new JsonObject();
        entry4.addProperty("desc:", "Set the chat notification type, one of these: PLAYER(closest player), EVERYONE(all players), NONE, OP(operator). default: PLAYER");
        entry4.addProperty("chatnotification", chatnotification);
        root.add("chatnotification", entry4);

        return root;
    }

    public void deserialize(JsonObject data)
    {
        try
        {
            if (data.has("reportInterval"))
            {
                reportInterval = data.get("reportInterval").getAsJsonObject().get("reportInterval").getAsInt();
            }
            if (data.has("chatnotification"))
            {
                chatnotification = data.get("chatnotification").getAsJsonObject().get("chatnotification").getAsString();
            }
            if (data.has("highlightitems"))
            {
                highlightitems = data.get("highlightitems").getAsJsonObject().get("highlightitems").getAsBoolean();
            }
            if (data.has("improveItemPerformance"))
            {
                improveItemPerformance = data.get("improveItemPerformance").getAsJsonObject().get("improveItemPerformance").getAsBoolean();
            }
            if (data.has("reportThreshold"))
            {
                reportThreshold = data.get("reportThreshold").getAsJsonObject().get("reportThreshold").getAsInt();
            }
            if (data.has("autoremovethreshold"))
            {
                autoremovethreshold = data.get("autoremovethreshold").getAsJsonObject().get("autoremovethreshold").getAsInt();
            }
        }
        catch (Exception e)
        {
            Leaky.LOGGER.error("Failed to deserialize config, using defaults: {}", e.getMessage(), e);
        }

        if (reportInterval < 1)
        {
            Leaky.LOGGER.warn("reportInterval must be >= 1, got {}. Resetting to default 180", reportInterval);
            reportInterval = 60 * 3;
        }
        if (reportThreshold < 1)
        {
            Leaky.LOGGER.warn("reportThreshold must be >= 1, got {}. Resetting to default 200", reportThreshold);
            reportThreshold = 200;
        }
        if (autoremovethreshold < reportThreshold)
        {
            Leaky.LOGGER.warn("autoremovethreshold must be >= reportThreshold, got {}. Resetting to default 400", autoremovethreshold);
            autoremovethreshold = 400;
        }
        if (!chatnotification.equalsIgnoreCase("PLAYER") && !chatnotification.equalsIgnoreCase("EVERYONE")
            && !chatnotification.equalsIgnoreCase("OP") && !chatnotification.equalsIgnoreCase("NONE"))
        {
            Leaky.LOGGER.warn("Invalid chatnotification value: '{}'. Resetting to default PLAYER", chatnotification);
            chatnotification = "PLAYER";
        }
    }
}
