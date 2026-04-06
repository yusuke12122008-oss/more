package me.earth.earthhack.impl.modules.client.konas;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.*;
import me.earth.earthhack.impl.managers.render.TextRenderer;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.client.konas.components.*;
import me.earth.earthhack.impl.util.render.ColorUtil;

import java.awt.*;

/**
 * KonasHUD - A clean, modular HUD system inspired by Konas / KonasClient.
 *
 * Components
 * ----------
 *  - ArrayList  : enabled modules, right side, slide animation
 *  - Info       : FPS / Ping / TPS / Speed, top-left
 *  - Coordinates: XYZ + dimension + conversion, bottom-left
 *  - Armor      : armour icons + durability bars, above hotbar
 *  - Totems     : totem count near hotbar
 *
 * Colour modes
 * ------------
 *  - Solid      : single colour chosen by the Color setting
 *  - Rainbow    : smooth animated rainbow based on Y position
 *  - Custom     : per-component overrides (future)
 */
public class KonasHUD extends Module
{
    public static final TextRenderer RENDERER = Managers.TEXT;

    // ------------------------------------------------------------------ //
    //  Settings
    // ------------------------------------------------------------------ //

    protected final Setting<KonasColorMode> colorMode =
            register(new EnumSetting<>("ColorMode", KonasColorMode.Rainbow));

    protected final Setting<Color> color =
            register(new ColorSetting("Color", new Color(100, 200, 255)));

    protected final Setting<Integer> rainbowSpeed =
            register(new NumberSetting<>("RainbowSpeed", 3000, 500, 10000));

    protected final Setting<Boolean> arrayList =
            register(new BooleanSetting("ArrayList", true));

    protected final Setting<Boolean> info =
            register(new BooleanSetting("Info", true));

    protected final Setting<Boolean> showFps =
            register(new BooleanSetting("FPS", true));

    protected final Setting<Boolean> showPing =
            register(new BooleanSetting("Ping", true));

    protected final Setting<Boolean> showTps =
            register(new BooleanSetting("TPS", true));

    protected final Setting<Boolean> showSpeed =
            register(new BooleanSetting("Speed", true));

    protected final Setting<Boolean> coordinates =
            register(new BooleanSetting("Coordinates", true));

    protected final Setting<Boolean> armor =
            register(new BooleanSetting("Armor", true));

    protected final Setting<Boolean> totems =
            register(new BooleanSetting("Totems", true));

    protected final Setting<Boolean> clientTag =
            register(new BooleanSetting("ClientTag", true));

    // ------------------------------------------------------------------ //
    //  Components
    // ------------------------------------------------------------------ //

    private final ArrayListComponent  arrayListComp;
    private final InfoComponent       infoComp;
    private final CoordinatesComponent coordComp;
    private final ArmorComponent      armorComp;
    private final TotemComponent      totemComp;

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //

    public KonasHUD()
    {
        super("KonasHUD", Category.Client);

        arrayListComp = new ArrayListComponent(this);
        infoComp      = new InfoComponent(this);
        coordComp     = new CoordinatesComponent(this);
        armorComp     = new ArmorComponent(this);
        totemComp     = new TotemComponent(this);

        this.listeners.add(new KonasListenerRender(this));
        this.setData(new KonasHUDData(this));
    }

    // ------------------------------------------------------------------ //
    //  Render entry-point called by KonasListenerRender
    // ------------------------------------------------------------------ //

    public void render(int sw, int sh)
    {
        if (mc.player == null || mc.world == null) return;

        // ---- Client tag (top-left) ----
        if (clientTag.getValue())
        {
            String tag = "\u00a77[\u00a7b3arthh4ck\u00a77]";
            RENDERER.drawStringWithShadow(tag, 2, 2, 0xFFFFFFFF);
        }

        // ---- Sync component toggles with settings ----
        arrayListComp.setVisible(arrayList.getValue());
        infoComp.setVisible(info.getValue());
        infoComp.showFps   = showFps.getValue();
        infoComp.showPing  = showPing.getValue();
        infoComp.showTps   = showTps.getValue();
        infoComp.showSpeed = showSpeed.getValue();
        coordComp.setVisible(coordinates.getValue());
        armorComp.setVisible(armor.getValue());
        totemComp.setVisible(totems.getValue());

        // ---- Render each visible component ----
        if (arrayListComp.isVisible())  arrayListComp.render(sw, sh);
        if (infoComp.isVisible())       infoComp.render(sw, sh);
        if (coordComp.isVisible())      coordComp.render(sw, sh);
        if (armorComp.isVisible())      armorComp.render(sw, sh);
        if (totemComp.isVisible())      totemComp.render(sw, sh);
    }

    // ------------------------------------------------------------------ //
    //  Colour helpers used by components
    // ------------------------------------------------------------------ //

    /**
     * Returns the accent/text colour for the given Y position.
     * In Rainbow mode colours shift per Y so the array list gets
     * a flowing gradient effect.
     */
    public int getTextColor(float y)
    {
        switch (colorMode.getValue())
        {
            case Rainbow:
                return ColorUtil.staticRainbow(y * 0.5f + System.currentTimeMillis() * 0.001f
                        * (rainbowSpeed.getValue() / 1000.0f), color.getValue());
            case Solid:
            default:
                return color.getValue().getRGB();
        }
    }

    /**
     * Returns an ARGB accent colour (fully opaque) suitable for bars/borders.
     */
    public int getRainbowColor(float y)
    {
        int rgb = getTextColor(y);
        // ensure full alpha
        return (rgb & 0x00FFFFFF) | 0xFF000000;
    }

    // ------------------------------------------------------------------ //
    //  Colour mode enum
    // ------------------------------------------------------------------ //

    public enum KonasColorMode
    {
        Solid,
        Rainbow
    }
}
