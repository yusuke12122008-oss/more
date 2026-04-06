package me.earth.earthhack.impl.modules.render.notification;

import net.minecraft.client.Minecraft;

/**
 * Represents a single notification popup.
 *
 * Animation fields
 * ----------------
 *  offsetX      - horizontal slide offset (starts large, eases to 0)
 *  slideAlpha   - 0..1 fade used during slide-in / slide-out
 *  targetY      - desired Y after stacking
 *  currentY     - interpolated current Y
 *  ticksRemaining / ticksMax - for progress bar calculation
 */
public class NotificationItem
{
    public final NotificationType type;
    public final String           message;

    // Animation
    public float offsetX;       // pixels to the right of final position
    public float slideAlpha;    // 0 = fully hidden, 1 = fully visible
    public float targetY;
    public float currentY;

    // Lifetime
    public float ticksRemaining;
    public float ticksMax;

    // Phase flags
    public boolean slidingIn  = true;
    public boolean slidingOut = false;

    public NotificationItem(NotificationType type,
                            String           message,
                            int              durationSeconds)
    {
        this.type    = type;
        this.message = message;

        // Start off-screen to the right
        this.offsetX      = 300.0f;
        this.slideAlpha   = 0.0f;
        this.targetY      = 0.0f;
        this.currentY     = 0.0f;

        int fps = Minecraft.getDebugFPS();
        if (fps <= 0) fps = 60;
        this.ticksRemaining = durationSeconds * fps;
        this.ticksMax       = this.ticksRemaining;
    }
}
