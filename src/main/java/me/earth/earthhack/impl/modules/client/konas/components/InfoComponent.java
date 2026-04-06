package me.earth.earthhack.impl.modules.client.konas.components;

import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.client.konas.KonasHUD;
import me.earth.earthhack.impl.util.math.MathUtil;
import me.earth.earthhack.impl.util.network.ServerUtil;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import net.minecraft.client.Minecraft;

import java.awt.*;

/**
 * Konas-style Info panel.
 * Shows FPS / Ping / TPS / Speed in a small box at the top-left,
 * just below the client tag.
 *
 *  +-----------+
 *  | FPS   120 |
 *  | Ping   14 |
 *  | TPS  19.8 |
 *  | Spd  12.3 |
 *  +-----------+
 */
public class InfoComponent extends HudComponent
{
    private static final int PAD      = 4;
    private static final int BG_COLOR = new Color(15, 15, 15, 160).getRGB();
    private static final int COL_GAP  = 30;   // gap between label and value

    // Whether each line is shown — controlled by KonasHUD settings
    public boolean showFps   = true;
    public boolean showPing  = true;
    public boolean showTps   = true;
    public boolean showSpeed = true;

    public InfoComponent(KonasHUD hud)
    {
        super(hud, "Info", true);
    }

    @Override
    public void render(int sw, int sh)
    {
        // Build list of (label, value) pairs
        java.util.List<String[]> rows = new java.util.ArrayList<>();

        if (showFps)
            rows.add(new String[]{ "FPS",  String.valueOf(Minecraft.getDebugFPS()) });
        if (showPing)
            rows.add(new String[]{ "Ping", ServerUtil.getPing() + " ms" });
        if (showTps)
        {
            String tpsVal = MathUtil.round(Managers.TPS.getTps(), 1)
                    + " [" + MathUtil.round(Managers.TPS.getCurrentTps(), 1) + "]";
            rows.add(new String[]{ "TPS", tpsVal });
        }
        if (showSpeed)
            rows.add(new String[]{ "Spd",  MathUtil.round(Managers.SPEED.getSpeed(), 2) + " km/h" });

        if (rows.isEmpty()) return;

        float lineH = KonasHUD.RENDERER.getStringHeightI() + 2;
        float bgH   = rows.size() * lineH + PAD * 2;

        // Determine widest row to set background width
        float maxW = 0;
        for (String[] row : rows)
        {
            float w = KonasHUD.RENDERER.getStringWidth(row[0])
                    + COL_GAP
                    + KonasHUD.RENDERER.getStringWidth(row[1]);
            if (w > maxW) maxW = w;
        }
        float bgW = maxW + PAD * 2 + 4;

        // Position: top-left, below the client tag (y=2+lineH+4)
        float x = PAD;
        float y = 2 + lineH + 6;

        // Background
        Render2DUtil.drawRect(x, y, x + bgW, y + bgH, BG_COLOR);
        // Left accent bar
        Render2DUtil.drawRect(x, y, x + 2, y + bgH, hud.getRainbowColor(y));

        float tx = x + PAD + 2;
        float ty = y + PAD;
        for (String[] row : rows)
        {
            // Label (grey)
            KonasHUD.RENDERER.drawStringWithShadow(row[0], tx, ty, 0xFFAAAAAA);
            // Value (accent color)
            float vx = tx + KonasHUD.RENDERER.getStringWidth(row[0]) + COL_GAP
                       - KonasHUD.RENDERER.getStringWidth(row[1]);
            // right-align value within the box
            vx = tx + maxW - KonasHUD.RENDERER.getStringWidth(row[1]);
            KonasHUD.RENDERER.drawStringWithShadow(row[1], vx, ty, hud.getTextColor(ty));
            ty += lineH;
        }
    }
}
