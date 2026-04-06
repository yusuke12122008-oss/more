package me.earth.earthhack.impl.modules.render.offhandshake;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import net.minecraft.client.renderer.ItemRenderer;

import java.lang.reflect.Field;

/**
 * Shakes the offhand item (totem, etc.) when the player attacks an End Crystal.
 * Achieved by resetting equippedProgressOffHand in ItemRenderer via reflection.
 */
public class OffhandShake extends Module
{
    protected final Setting<Float> amount =
            register(new NumberSetting<>("Amount", 0.0f, 0.0f, 1.0f));
    protected final Setting<Float> speed =
            register(new NumberSetting<>("Speed", 10.0f, 1.0f, 20.0f));

    /** Cached reflection field for equippedProgressOffHand. */
    protected Field fieldEquippedProgressOffHand;

    public OffhandShake()
    {
        super("OffhandShake", Category.Render);
        this.listeners.add(new ListenerOffhandPacket(this));
        this.setData(new OffhandShakeData(this));
        resolveField();
    }

    /**
     * Resolve the private field once at construction time.
     * Tries MCP name first, then falls back to SRG (obfuscated) name.
     */
    private void resolveField()
    {
        try
        {
            fieldEquippedProgressOffHand =
                    ItemRenderer.class.getDeclaredField("equippedProgressOffHand");
            fieldEquippedProgressOffHand.setAccessible(true);
        }
        catch (NoSuchFieldException e)
        {
            try
            {
                // SRG name for 1.12.2 equippedProgressOffHand
                fieldEquippedProgressOffHand =
                        ItemRenderer.class.getDeclaredField("field_187471_h");
                fieldEquippedProgressOffHand.setAccessible(true);
            }
            catch (NoSuchFieldException ex)
            {
                System.err.println(
                        "[OffhandShake] Could not resolve equippedProgressOffHand field!");
                fieldEquippedProgressOffHand = null;
            }
        }
    }

    @Override
    public String getDisplayInfo()
    {
        return String.format("%.1f", amount.getValue());
    }

    /**
     * Sets equippedProgressOffHand to the configured Amount value,
     * causing the offhand item to visually dip and recover.
     */
    protected void applyShake()
    {
        if (fieldEquippedProgressOffHand == null)
        {
            return;
        }

        try
        {
            ItemRenderer renderer = mc.entityRenderer.itemRenderer;
            fieldEquippedProgressOffHand.setFloat(renderer, amount.getValue());
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }
}
