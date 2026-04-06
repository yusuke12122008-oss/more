package me.earth.earthhack.impl.modules.render.notification;

import me.earth.earthhack.api.util.interfaces.Globals;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import net.minecraft.client.gui.ScaledResolution;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages the notification queue and renders popup notifications.
 * Design ported from a reference with colored backgrounds per type,
 * title + message layout, left accent bar, and progress bar.
 */
public class NotificationRenderer implements Globals {
    private final List<NotificationItem> notifications =
            new CopyOnWriteArrayList<>();
    private final Notification module;

    /* Layout constants */
    private static final float BOX_PADDING_X = 8.0f;
    private static final float BOX_PADDING_Y = 4.0f;
    private static final float ACCENT_BAR_WIDTH = 3.0f;
    private static final float MIN_BOX_WIDTH = 160.0f;
    private static final float BOX_MARGIN_BOTTOM = 5.0f;

    public NotificationRenderer(Notification module) {
        this.module = module;
    }

    /* --- Public API --- */

    public void show(NotificationType type, String message) {
        if (mc.player == null || !module.isEnabled()) return;

        int textHeight = Managers.TEXT.getStringHeightI();
        float boxH = getBoxHeight(textHeight);

        // Push existing notifications upward
        for (NotificationItem n : notifications) {
            n.targetY -= (boxH + BOX_MARGIN_BOTTOM);
        }

        ScaledResolution sr = new ScaledResolution(mc);
        NotificationItem item =
                new NotificationItem(type, message, module.time.getValue());
        item.targetY  = sr.getScaledHeight() - boxH - 10;
        item.currentY = sr.getScaledHeight() - boxH - 10;
        notifications.add(item);
    }

    public void clear() {
        notifications.clear();
    }

    /* --- Render (called each frame) --- */

    public void onRender2D() {
        if (mc.player == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int screenW = sr.getScaledWidth();
        int textH   = Managers.TEXT.getStringHeightI();

        for (NotificationItem n : notifications) {
            renderSingle(n, screenW, textH);
            updateAnimation(n);
        }

        // Remove finished notifications
        notifications.removeIf(n ->
                n.offsetX > 400.0f && n.ticksRemaining <= 0);
    }

    /* --- Internals --- */

    private void renderSingle(NotificationItem n,
                              int screenW, int textH) {
        String title = n.type.getTitle();
        int titleW   = Managers.TEXT.getStringWidth(title);
        int msgW     = Managers.TEXT.getStringWidth(n.message);

        float contentW = Math.max(titleW, msgW);
        float boxW = Math.max(contentW + BOX_PADDING_X * 2
                              + ACCENT_BAR_WIDTH + 4, MIN_BOX_WIDTH);
        float boxH = getBoxHeight(textH);

        // Box position (right-aligned, offset by animation)
        float x1 = screenW - boxW - 6 + n.offsetX;
        float y1 = n.currentY;
        float x2 = screenW - 6 + n.offsetX;
        float y2 = y1 + boxH;

        // -- Background --
        Render2DUtil.drawRect(x1, y1, x2, y2,
                n.type.getBackgroundColor());

        // -- Left accent bar --
        Render2DUtil.drawRect(x1, y1, x1 + ACCENT_BAR_WIDTH, y2,
                n.type.getAccentColor());

        // -- Title text (bold-ish via shadow) --
        float textX = x1 + ACCENT_BAR_WIDTH + 6;
        float titleY = y1 + BOX_PADDING_Y;
        Managers.TEXT.drawStringWithShadow(
                title, textX, titleY, 0xFFFFFFFF);

        // -- Message text --
        float msgY = titleY + textH + 2;
        Managers.TEXT.drawStringWithShadow(
                n.message, textX, msgY, 0xFFDDDDDD);

        // -- Progress bar at bottom --
        float progress = n.ticksMax > 0
                ? (n.ticksMax - n.ticksRemaining) / n.ticksMax
                : 1.0f;
        float barFullW = boxW;
        float barW = barFullW * (1.0f - progress);

        // bar background (darker)
        Render2DUtil.drawRect(x1, y2 - 2, x2, y2,
                0x44000000);
        // bar foreground
        Render2DUtil.drawRect(x1, y2 - 2, x1 + barW, y2,
                0xDDFFFFFF);
    }

    private void updateAnimation(NotificationItem n) {
        if (n.ticksRemaining <= 0) {
            // Slide out to the right
            n.offsetX += (500.0f - n.offsetX) / 10.0f;
        } else {
            n.ticksRemaining--;
            // Slide in from the right
            n.offsetX += (0.0f - n.offsetX) / 4.0f;
            // Smooth Y interpolation
            n.currentY += (n.targetY - n.currentY) / 4.0f;
        }
    }

    private float getBoxHeight(int textH) {
        // title + message + padding
        return textH * 2 + BOX_PADDING_Y * 2 + 4;
    }
}
