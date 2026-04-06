package me.earth.earthhack.impl.modules.render.oldanimation;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;

/**
 * Changes animations to look like 1.8 style.
 * Settings are read from Mixins / other parts of the codebase.
 */
public class OldAnimation extends Module
{
    public final Setting<Boolean> attackBob =
            register(new BooleanSetting("AttackBob", true));
    public final Setting<Boolean> switchBob =
            register(new BooleanSetting("SwitchBob", true));
    public final Setting<Boolean> swordBlock =
            register(new BooleanSetting("SwordBlock", true));
    public final Setting<Float> x =
            register(new NumberSetting<>("X", 0.0f, -1.0f, 1.0f));
    public final Setting<Float> y =
            register(new NumberSetting<>("Y", 0.0f, -1.0f, 1.0f));
    public final Setting<Float> z =
            register(new NumberSetting<>("Z", 0.0f, -1.0f, 1.0f));

    private static OldAnimation instance;

    public OldAnimation()
    {
        super("OldAnimation", Category.Render);
        this.setData(new OldAnimationData(this));
        instance = this;
    }

    public static OldAnimation getInstance()
    {
        if (instance == null)
        {
            instance = new OldAnimation();
        }
        return instance;
    }
}
