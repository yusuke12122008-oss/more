package me.earth.earthhack.impl.modules.client.konas.components;

import me.earth.earthhack.impl.modules.client.konas.KonasHUD;
import me.earth.earthhack.impl.util.minecraft.DamageUtil;
import me.earth.earthhack.impl.util.render.ColorHelper;
import me.earth.earthhack.impl.util.render.Render2DUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;

import java.awt.*;

/**
 * Konas-style Armor display.
 * Shows 4 armor pieces centred above the hotbar, each with:
 *   - item icon
 *   - durability percentage bar below the icon (color: green -> yellow -> red)
 *   - percentage text
 */
public class ArmorComponent extends HudComponent
{
    private static final int SLOT_SIZE = 18;
    private static final int BAR_H     = 2;
    private static final int PAD       = 2;
    private static final int BG_COLOR  = new Color(15, 15, 15, 160).getRGB();

    public ArmorComponent(KonasHUD hud)
    {
        super(hud, "Armor", true);
    }

    @Override
    public void render(int sw, int sh)
    {
        if (mc.player == null) return;

        // Collect non-empty armor slots (head -> feet, left-to-right)
        java.util.List<ItemStack> pieces = new java.util.ArrayList<>();
        for (int i = 3; i >= 0; i--)
        {
            ItemStack stack = mc.player.inventory.armorInventory.get(i);
            if (!stack.isEmpty()) pieces.add(stack);
        }
        if (pieces.isEmpty()) return;

        int count  = pieces.size();
        int totalW = count * SLOT_SIZE + (count - 1) * PAD;
        int startX = sw / 2 - totalW / 2;
        int y      = sh - getArmorBaseY() - SLOT_SIZE - BAR_H - 4;

        // Background
        Render2DUtil.drawRect(startX - PAD, y - PAD,
                startX + totalW + PAD, y + SLOT_SIZE + BAR_H + 6,
                BG_COLOR);
        // Top accent bar
        Render2DUtil.drawRect(startX - PAD, y - PAD,
                startX + totalW + PAD, y - PAD + 2,
                hud.getRainbowColor(y));

        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.disableDepth();

        int x = startX;
        for (ItemStack stack : pieces)
        {
            // Item icon
            mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
            mc.getRenderItem().renderItemOverlays(mc.fontRenderer, stack, x, y);

            // Durability fraction
            float pct = DamageUtil.getPercent(stack) / 100.0f;
            int   barW = (int) (SLOT_SIZE * pct);
            int   barColor = ColorHelper.toColor(pct * 120.0f, 100.0f, 50.0f, 1.0f).getRGB();

            int barY = y + SLOT_SIZE + 1;
            // Background track
            Render2DUtil.drawRect(x, barY, x + SLOT_SIZE, barY + BAR_H,
                    new Color(50, 50, 50, 200).getRGB());
            // Fill
            if (barW > 0)
                Render2DUtil.drawRect(x, barY, x + barW, barY + BAR_H, barColor);

            // Percentage text (tiny, below the bar)
            String pctStr = (int)(pct * 100) + "%";
            float  tw     = KonasHUD.RENDERER.getStringWidth(pctStr);
            KonasHUD.RENDERER.drawStringWithShadow(pctStr,
                    x + (SLOT_SIZE - tw) / 2f,
                    barY + BAR_H + 1,
                    barColor);

            x += SLOT_SIZE + PAD;
        }

        GlStateManager.enableDepth();
        RenderHelper.disableStandardItemLighting();
    }

    /** Returns the vanilla hotbar Y-offset so armor sits just above it. */
    private int getArmorBaseY()
    {
        if (mc.player.isInsideOfMaterial(net.minecraft.block.material.Material.WATER)
                && mc.player.getAir() > 0
                && !mc.player.capabilities.isCreativeMode)
            return 65;
        if (mc.player.capabilities.isCreativeMode)
            return mc.player.isRidingHorse() ? 45 : 38;
        return 55;
    }
}
