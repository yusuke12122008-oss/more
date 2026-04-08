package me.earth.earthhack.impl.modules.client.konas.components;

import me.earth.earthhack.impl.modules.client.konas.KonasHUD;
import me.earth.earthhack.impl.util.minecraft.InventoryUtil;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import java.awt.*;

/**
 * Konas-style Totem counter.
 * Shows the totem item icon with a count badge, centered near the hotbar.
 *
 *   [totem icon]
 *          x12
 */
public class TotemComponent extends HudComponent
{
    private static final int ICON_SIZE = 16;
    private static final int BG_COLOR  = new Color(15, 15, 15, 160).getRGB();
    private static final int PAD       = 3;

    // X/Y offset from center-bottom (in pixels)
    public int offsetX = -40;
    public int offsetY = 0;

    public TotemComponent(KonasHUD hud)
    {
        super(hud, "Totems", true);
    }

    @Override
    public void render(int sw, int sh)
    {
        if (mc.player == null) return;

        int count = InventoryUtil.getCount(Items.TOTEM_OF_UNDYING);
        if (count <= 0) return;

        int cx = sw / 2 + offsetX;
        int cy = sh - getArmorBaseY() - ICON_SIZE - PAD * 2;

        // Background box
        Render2DUtil.drawRect(cx - PAD, cy - PAD,
                cx + ICON_SIZE + PAD, cy + ICON_SIZE + PAD + 8,
                BG_COLOR);
        // Top accent
        Render2DUtil.drawRect(cx - PAD, cy - PAD,
                cx + ICON_SIZE + PAD, cy - PAD + 2,
                hud.getRainbowColor(cy));

        // Item icon
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.disableDepth();
        mc.getRenderItem().renderItemAndEffectIntoGUI(
                new ItemStack(Items.TOTEM_OF_UNDYING), cx, cy);
        mc.getRenderItem().renderItemOverlays(mc.fontRenderer,
                new ItemStack(Items.TOTEM_OF_UNDYING), cx, cy);
        GlStateManager.enableDepth();
        RenderHelper.disableStandardItemLighting();

        // Count badge (bottom-right of icon)
        String countStr = "x" + count;
        float tw = KonasHUD.RENDERER.getStringWidth(countStr);
        KonasHUD.RENDERER.drawStringWithShadow(countStr,
                cx + ICON_SIZE - tw + PAD,
                cy + ICON_SIZE + 1,
                count == 1 ? 0xFFFF5555   // red when last totem
                           : hud.getTextColor(cy));
    }

    private int getArmorBaseY()
    {
        if (mc.player == null) return 55;
        if (mc.player.isInsideOfMaterial(net.minecraft.block.material.Material.WATER)
                && mc.player.getAir() > 0
                && !mc.player.capabilities.isCreativeMode) return 65;
        if (mc.player.capabilities.isCreativeMode)
            return mc.player.isRidingHorse() ? 45 : 38;
        return 55;
    }
}
