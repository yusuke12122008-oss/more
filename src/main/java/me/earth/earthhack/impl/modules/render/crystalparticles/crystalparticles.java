package me.earth.earthhack.impl.modules.render.crystalparticles;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;

/**
 * Spawns crit/magic particles when attacking an Ender Crystal.
 */
public class CrystalParticles extends Module
{
    protected final Setting<Boolean> critParticles =
            register(new BooleanSetting("CritParticles", true));
    protected final Setting<Boolean> magicParticles =
            register(new BooleanSetting("MagicParticles", false));

    public CrystalParticles()
    {
        super("CrystalParticles", Category.Render);
        this.listeners.add(new ListenerPacketSend(this));
        this.setData(new CrystalParticlesData(this));
    }
}
