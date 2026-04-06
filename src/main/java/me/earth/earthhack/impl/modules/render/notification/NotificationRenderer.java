package me.earth.earthhack.impl.modules.render.notification;

import me.earth.earthhack.api.util.interfaces.Globals;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CandyPlus-style notification renderer.
 *
 * Visual anatomy of one notification card
 * ----------------------------------------
 *
 *   +--+------------------------------------------+
 *   |  |  [icon] Title                            |
 *   |  |  Message text                            |
 *   |  +------------------------------------------+
 *   |  | [============================------]     |  <- progress bar (type color)
 *   +--+------------------------------------------+
 *    ^
 *    3 px accent bar (type color, full height)
 *
 * Details
 * -------
 *  - Dark semi-transparent rounded background (radius = 3)
 *  - 3 px left accent bar in type color
 *  - Icon glyph + title on first line (type color for icon, white for title)
 *  - Message text on second line (light grey)
 *  - Rounded progress bar at bottom; shrinks left-to-right as time passes,
 *    turns slightly transparent when nearly expired
 *  - Smooth ease-out slide-in from right
 *  - Ease-in slide-out to right when expired
 *  - Notifications stack upward (each new one pushes older ones up)
 *
 * Position: bottom-right corner, 8 px margin
 */
public class NotificationRenderer implements Globals
{
    // ------------------------------------------------------------------ //
    //  State
    // ------------------------------------------------------------------ //
    private final List<NotificationItem> notifications =
            new CopyOnWriteArrayList<>();
    private final Notification module;

    // ------------------------------------------------------------------ //
    //  Layout constants
    // ------------------------------------------------------------------ //
    private static final float CORNER_RADIUS    = 3.0f;
    private static final float ACCENT_W         = 3.0f;
    private static final float PAD_X            = 8.0f;   // inside padding (after accent)
    private static final float PAD_Y            = 5.0f;   // top/bottom inner padding
    private static final float MIN_WIDTH        = 170.0f;
    private static final float GAP_BETWEEN      = 4.0f;   // vertical gap between cards
    private static final float SCREEN_MARGIN    = 8.0f;   // distance from screen edge
    private static final float PROGRESS_H       = 3.0f;   // progress bar height
    private static final float PROGRESS_RADIUS  = 1.5f;
    private static final float LINE_GAP         = 2.0f;   // gap between title and message

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //
    public NotificationRenderer(Notification module)
    {
        this.module = module;
    }

<<<<<<< HEAD
    /* --- Public API --- */
=======
    // ------------------------------------------------------------------ //
    //  Public API
    // ------------------------------------------------------------------ //
>>>>>>> 471e7719ef297af2aa456053751136ecc17045be

    public void show(NotificationType type, String message)
    {
        if (mc.player == null || !module.isEnabled()) return;

        float boxH = getBoxHeight();

        // Push all existing notifications upward
        for (NotificationItem n : notifications)
        {
            n.targetY -= (boxH + GAP_BETWEEN);
        }

        ScaledResolution sr = new ScaledResolution(mc);
        NotificationItem item =
                new NotificationItem(type, message, module.time.getValue());

        float startY = sr.getScaledHeight() - boxH - SCREEN_MARGIN;
        item.targetY  = startY;
        item.currentY = startY;
        notifications.add(item);
    }

    public void clear()
    {
        notifications.clear();
    }

<<<<<<< HEAD
    /* --- Render (called each frame) --- */
=======
    // ------------------------------------------------------------------ //
    //  Render entry-point (called each frame)
    // ------------------------------------------------------------------ //
>>>>>>> 471e7719ef297af2aa456053751136ecc17045be

    public void onRender2D()
    {
        if (mc.player == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();

        for (NotificationItem n : notifications)
        {
            tick(n);
            renderCard(n, sw);
        }

        // Remove cards that have fully slid off screen
        notifications.removeIf(n -> n.slidingOut && n.offsetX > 350.0f);
    }

<<<<<<< HEAD
    /* --- Internals --- */
=======
    // ------------------------------------------------------------------ //
    //  Per-frame tick: update animation state
    // ------------------------------------------------------------------ //
>>>>>>> 471e7719ef297af2aa456053751136ecc17045be

    private void tick(NotificationItem n)
    {
        int fps = Math.max(1, net.minecraft.client.Minecraft.getDebugFPS());
        float lerpFactor = Math.min(1.0f, 12.0f / fps);  // ~12 px / frame at 60 fps

        if (n.slidingIn)
        {
            // Ease-out slide in: offsetX -> 0
            n.offsetX   = lerp(n.offsetX,  0.0f,  lerpFactor);
            n.slideAlpha = lerp(n.slideAlpha, 1.0f, lerpFactor);

            if (n.offsetX < 0.5f)
            {
                n.offsetX    = 0.0f;
                n.slideAlpha = 1.0f;
                n.slidingIn  = false;
            }
        }
        else if (!n.slidingOut)
        {
            // Countdown
            n.ticksRemaining--;
            if (n.ticksRemaining <= 0)
            {
                n.slidingOut = true;
            }
        }
        else
        {
            // Ease-in slide out: offsetX -> large positive
            n.offsetX    = lerp(n.offsetX, 400.0f, lerpFactor * 0.7f);
            n.slideAlpha = lerp(n.slideAlpha, 0.0f, lerpFactor);
        }

        // Smooth Y stacking
        n.currentY = lerp(n.currentY, n.targetY, lerpFactor);
    }

    // ------------------------------------------------------------------ //
    //  Draw a single notification card
    // ------------------------------------------------------------------ //

    private void renderCard(NotificationItem n, int sw)
    {
        float textH = Managers.TEXT.getStringHeightI();
        float boxH  = getBoxHeight();
        float boxW  = getBoxWidth(n);

        // Right-aligned; shift right by offsetX during animation
        float x1 = sw - boxW - SCREEN_MARGIN + n.offsetX;
        float y1 = n.currentY;
        float x2 = sw       - SCREEN_MARGIN + n.offsetX;
        float y2 = y1 + boxH;

<<<<<<< HEAD
        // -- Background --
        Render2DUtil.drawRect(x1, y1, x2, y2,
                n.type.getBackgroundColor());

        // -- Left accent bar --
        Render2DUtil.drawRect(x1, y1, x1 + ACCENT_BAR_WIDTH, y2,
                n.type.getAccentColor());

        // -- Title text (bold-ish via shadow) --
        float textX = x1 + ACCENT_BAR_WIDTH + 6;
        float titleY = y1 + BOX_PADDING_Y;
=======
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);

        int accent = n.type.getAccentColor();
        int bg     = n.type.getBackgroundColor();

        // ---- Outer card shadow (subtle dark glow) ----
        int shadow = 0x30000000;
        Render2DUtil.roundedRect(x1 - 1, y1 - 1, x2 + 1, y2 + 1,
                CORNER_RADIUS + 1, shadow);

        // ---- Background (dark rounded rect) ----
        Render2DUtil.roundedRect(x1, y1, x2, y2, CORNER_RADIUS, bg);

        // ---- Left accent bar ----
        // We draw a solid rect then clip it to the left edge of the card
        float barX2 = x1 + ACCENT_W;
        // Left side: draw with round corners only on the left
        Render2DUtil.drawRect(x1, y1 + CORNER_RADIUS,
                              barX2, y2 - CORNER_RADIUS, accent);
        Render2DUtil.drawQuarterCircle(x1, y1 + CORNER_RADIUS,
                CORNER_RADIUS - 0.2f, accent, 3);
        Render2DUtil.drawQuarterCircle(x1, y2 - CORNER_RADIUS,
                CORNER_RADIUS - 0.2f, accent, 4);
        // Fill corners between bar and rounded bg
        Render2DUtil.drawRect(barX2, y1, x2, y1 + CORNER_RADIUS, bg);
        Render2DUtil.drawRect(barX2, y2 - CORNER_RADIUS, x2, y2, bg);

        // ---- Thin separator between accent bar and content ----
        Render2DUtil.drawRect(barX2, y1 + CORNER_RADIUS,
                barX2 + 1, y2 - CORNER_RADIUS - PROGRESS_H - 1,
                0x22FFFFFF);

        // ---- Content area ----
        float contentX = x1 + ACCENT_W + PAD_X;
        float titleY   = y1 + PAD_Y;

        // Icon (type accent color)
        String icon = n.type.getIcon();
        Managers.TEXT.drawStringWithShadow(icon, contentX, titleY, accent);
        float iconW = Managers.TEXT.getStringWidth(icon) + 4;

        // Title (white)
>>>>>>> 471e7719ef297af2aa456053751136ecc17045be
        Managers.TEXT.drawStringWithShadow(
                n.type.getTitle(), contentX + iconW, titleY, 0xFFFFFFFF);

<<<<<<< HEAD
        // -- Message text --
        float msgY = titleY + textH + 2;
=======
        // Message (light grey, second line)
        float msgY = titleY + textH + LINE_GAP;
>>>>>>> 471e7719ef297af2aa456053751136ecc17045be
        Managers.TEXT.drawStringWithShadow(
                n.message, contentX, msgY, 0xFFCCCCCC);

<<<<<<< HEAD
        // -- Progress bar at bottom --
        float progress = n.ticksMax > 0
=======
        // ---- Progress bar ----
        float progress  = n.ticksMax > 0
>>>>>>> 471e7719ef297af2aa456053751136ecc17045be
                ? (n.ticksMax - n.ticksRemaining) / n.ticksMax
                : 1.0f;
        // remaining fraction (bar shrinks from right to left)
        float remaining = 1.0f - progress;

        float barTop    = y2 - PROGRESS_H - 1;
        float barBottom = y2 - 1;
        float barLeft   = x1 + ACCENT_W + 1;
        float barRight  = x2 - 1;
        float barFull   = barRight - barLeft;

        // Track
        Render2DUtil.drawRect(barLeft, barTop, barRight, barBottom, 0x33FFFFFF);

        // Fill (shrinks to the left)
        if (remaining > 0.001f)
        {
            float fillRight = barLeft + barFull * remaining;
            // Normal fill
            Render2DUtil.drawGradientRect(
                    barLeft, barTop, fillRight, barBottom,
                    true,  // sideways gradient
                    blendAlpha(accent, 0xFF),
                    blendAlpha(accent, 0xBB));
        }

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    // ------------------------------------------------------------------ //
    //  Helpers
    // ------------------------------------------------------------------ //

    private float getBoxHeight()
    {
        float textH = Managers.TEXT.getStringHeightI();
        // title line + message line + top/bottom padding + progress bar + line gap
        return textH * 2 + PAD_Y * 2 + PROGRESS_H + LINE_GAP + 2;
    }

    private float getBoxWidth(NotificationItem n)
    {
        float iconW   = Managers.TEXT.getStringWidth(n.type.getIcon()) + 4;
        float titleW  = Managers.TEXT.getStringWidth(n.type.getTitle()) + iconW;
        float msgW    = Managers.TEXT.getStringWidth(n.message);
        float contentW = Math.max(titleW, msgW);
        return Math.max(contentW + ACCENT_W + PAD_X * 2 + 2, MIN_WIDTH);
    }

    /** Replace the alpha byte in an ARGB color. */
    private static int blendAlpha(int argb, int alpha)
    {
        return (argb & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    private static float lerp(float a, float b, float t)
    {
        return a + (b - a) * t;
    }
}
