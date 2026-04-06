package me.earth.earthhack.impl.modules.client.konas.components;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Hidden;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.client.konas.KonasHUD;
import me.earth.earthhack.impl.util.client.ModuleUtil;
import me.earth.earthhack.impl.util.render.ColorUtil;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import net.minecraft.client.Minecraft;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Konas-style ArrayList component.
 * Draws enabled modules sorted by name length on the right side of the screen.
 * Each entry has:
 *   - a coloured left accent bar
 *   - semi-transparent dark background
 *   - white module name with optional suffix
 *   - smooth slide-in animation
 */
public class ArrayListComponent extends HudComponent
{
    // Per-module animation state (current X offset from right edge)
    private final Map<Module, Float> animX = new LinkedHashMap<>();

    // Speed of the slide animation (pixels per frame at 60 fps)
    private static final float ANIM_SPEED = 0.18f;

    // Top padding from the top of the screen
    private static final int TOP_PAD = 4;
    // Vertical gap between entries
    private static final int LINE_GAP = 1;

    public ArrayListComponent(KonasHUD hud)
    {
        super(hud, "ArrayList", true);
    }

    @Override
    public void render(int sw, int sh)
    {
        final int fps = Math.max(1, Minecraft.getDebugFPS());

        // Gather enabled modules sorted by display-name length (longest first)
        List<Module> enabled = Managers.MODULE
                .getModules()
                .stream()
                .filter(m -> m.isEnabled() && m.isHidden() != Hidden.Hidden)
                .sorted(Comparator.comparingInt(
                        m -> -KonasHUD.RENDERER.getStringWidth(ModuleUtil.getHudName(m))))
                .collect(Collectors.toList());

        // Remove stale animation entries
        animX.keySet().removeIf(m -> !enabled.contains(m) && animX.get(m) >= sw);

        final float lineH  = KonasHUD.RENDERER.getStringHeightI() + 2;
        float y = TOP_PAD;

        for (Module module : enabled)
        {
            final String label     = ModuleUtil.getHudName(module);
            final float  textW     = KonasHUD.RENDERER.getStringWidth(label);
            final float  boxW      = textW + 6;          // 3px left+right padding
            final float  targetX   = sw - boxW;          // final resting X

            // Initialise off-screen if first frame
            animX.putIfAbsent(module, (float) sw);

            // Lerp toward target
            float cx = animX.get(module);
            cx += (targetX - cx) * Math.min(1.0f, ANIM_SPEED * (60.0f / fps));
            animX.put(module, cx);

            // ---- Accent bar colour (rainbow or solid) ----
            int accentColor = hud.getRainbowColor(y);

            // ---- Background ----
            Render2DUtil.drawRect(cx, y, sw, y + lineH,
                    new Color(15, 15, 15, 160).getRGB());

            // ---- Left accent bar (2 px wide) ----
            Render2DUtil.drawRect(cx, y, cx + 2, y + lineH, accentColor);

            // ---- Module name ----
            KonasHUD.RENDERER.drawStringWithShadow(label,
                    cx + 4,
                    y + 1,
                    hud.getTextColor(y));

            y += lineH + LINE_GAP;
        }

        // Slide-out disabled modules that still have animation state
        for (Map.Entry<Module, Float> e : new ArrayList<>(animX.entrySet()))
        {
            if (!enabled.contains(e.getKey()))
            {
                float cx = e.getValue();
                cx += (sw + 20 - cx) * Math.min(1.0f, ANIM_SPEED * (60.0f / fps));
                animX.put(e.getKey(), cx);
            }
        }
    }
}
