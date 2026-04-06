package me.earth.earthhack.impl.modules.render.notification;

import net.minecraft.client.Minecraft;

/**
 * Represents a single notification popup with type, title, and message.
 */
public class NotificationItem {
    public final NotificationType type;
    public final String message;
    public float offsetX;
    public float targetY;
    public float currentY;
    public float ticksRemaining;
    public float ticksMax;

    public NotificationItem(NotificationType type,
                            String message,
                            int durationSeconds) {
        this.type = type;
        this.message = message;
        this.offsetX = 300.0f;
        this.targetY = 0.0f;
        this.currentY = 0.0f;
        int fps = Minecraft.getDebugFPS();
        if (fps <= 0) fps = 60;
        this.ticksRemaining = durationSeconds * fps;
        this.ticksMax = this.ticksRemaining;
    }
}
