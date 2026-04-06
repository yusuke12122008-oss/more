package me.earth.earthhack.impl.modules.render.oldanimation;

import me.earth.earthhack.api.module.data.AbstractData;

final class OldAnimationData extends AbstractData<OldAnimation>
{
    public OldAnimationData(OldAnimation module)
    {
        super(module);
        descriptions.put(module.attackBob,
                "Enables 1.8-style attack bobbing.");
        descriptions.put(module.switchBob,
                "Enables 1.8-style item switch bobbing.");
        descriptions.put(module.swordBlock,
                "Enables 1.8-style sword blocking animation.");
        descriptions.put(module.x, "X offset for the animation.");
        descriptions.put(module.y, "Y offset for the animation.");
        descriptions.put(module.z, "Z offset for the animation.");
    }

    @Override
    public int getColor()
    {
        return 0xffff8000;
    }

    @Override
    public String getDescription()
    {
        return "Changes animations to look like 1.8.";
    }
}
