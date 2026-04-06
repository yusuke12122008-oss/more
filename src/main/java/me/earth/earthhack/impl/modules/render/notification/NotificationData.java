package me.earth.earthhack.impl.modules.render.notification;

import me.earth.earthhack.api.module.data.AbstractData;

final class NotificationData extends AbstractData<Notification> {

    public NotificationData(Notification module) {
        super(module);
        register(module.time,
                "How many seconds each notification stays on screen.");
        register(module.toggle,
                "Show notifications when modules are toggled.");
        register(module.totemPop,
                "Show notifications when a player pops a totem.");
        register(module.death,
                "Show notifications when a tracked player dies.");
    }

    @Override
    public int getColor() {
        return 0xffff6600;
    }

    @Override
    public String getDescription() {
        return "Displays colored popup notifications on screen.";
    }
}
