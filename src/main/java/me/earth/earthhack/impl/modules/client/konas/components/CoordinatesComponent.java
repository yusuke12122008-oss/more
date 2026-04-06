package me.earth.earthhack.impl.modules.client.konas.components;

import com.mojang.realmsclient.gui.ChatFormatting;
import me.earth.earthhack.impl.modules.client.konas.KonasHUD;
import me.earth.earthhack.impl.util.math.rotation.RotationUtil;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import net.minecraft.client.entity.EntityPlayerSP;

import java.awt.*;

/**
 * Konas-style Coordinates + Direction component.
 * Displayed at the bottom-left.
 *
 *  [>] X: 1234   Y: 64   Z: -512
 *      Overworld -> Nether: [154, -64]
 */
public class CoordinatesComponent extends HudComponent
{
    private static final int PAD  = 4;
    private static final int BG_COLOR = new Color(15, 15, 15, 160).getRGB();

    public CoordinatesComponent(KonasHUD hud)
    {
        super(hud, "Coordinates", true);
    }

    @Override
    public void render(int sw, int sh)
    {
        EntityPlayerSP p = mc.player;
        if (p == null) return;

        final long px = Math.round(p.posX);
        final long py = Math.round(p.posY);
        final long pz = Math.round(p.posZ);

        String dim;
        String netherCoords = "";
        switch (p.dimension)
        {
            case -1: // Nether
                dim = ChatFormatting.RED + "Nether";
                netherCoords = ChatFormatting.GRAY + "Overworld: ["
                        + ChatFormatting.WHITE + (px * 8) + ChatFormatting.GRAY
                        + ", " + ChatFormatting.WHITE + (pz * 8)
                        + ChatFormatting.GRAY + "]";
                break;
            case  1: // End
                dim = ChatFormatting.DARK_PURPLE + "The End";
                break;
            default: // Overworld
                dim = ChatFormatting.GREEN + "Overworld";
                netherCoords = ChatFormatting.GRAY + "Nether: ["
                        + ChatFormatting.WHITE + (px / 8) + ChatFormatting.GRAY
                        + ", " + ChatFormatting.WHITE + (pz / 8)
                        + ChatFormatting.GRAY + "]";
                break;
        }

        String dir    = RotationUtil.getDirection4D(false);
        String coords = "XYZ  "
                + ChatFormatting.WHITE + px + ChatFormatting.GRAY + "  "
                + ChatFormatting.WHITE + py + ChatFormatting.GRAY + "  "
                + ChatFormatting.WHITE + pz;

        float lineH = KonasHUD.RENDERER.getStringHeightI() + 2;
        int   lines = netherCoords.isEmpty() ? 2 : 3;
        float bgH   = lines * lineH + PAD;
        float bgW   = Math.max(
                KonasHUD.RENDERER.getStringWidth(coords),
                netherCoords.isEmpty() ? 0 : KonasHUD.RENDERER.getStringWidth(netherCoords))
                + PAD * 2 + 4;

        float x = PAD;
        float y = sh - bgH - PAD;

        // Background
        Render2DUtil.drawRect(x, y, x + bgW, y + bgH, BG_COLOR);
        // Accent bar on the left
        Render2DUtil.drawRect(x, y, x + 2, y + bgH, hud.getRainbowColor(y));

        float tx = x + PAD + 2;
        float ty = y + 2;

        // Dimension + direction
        KonasHUD.RENDERER.drawStringWithShadow(dim + "  " + ChatFormatting.GRAY + dir,
                tx, ty, hud.getTextColor(ty));
        ty += lineH;

        // Coordinates
        KonasHUD.RENDERER.drawStringWithShadow(coords, tx, ty, hud.getTextColor(ty));
        ty += lineH;

        // Nether/Overworld conversion
        if (!netherCoords.isEmpty())
        {
            KonasHUD.RENDERER.drawStringWithShadow(netherCoords, tx, ty, hud.getTextColor(ty));
        }
    }
}
