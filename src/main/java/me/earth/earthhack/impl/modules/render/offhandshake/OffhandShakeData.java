package me.earth.earthhack.impl.modules.render.offhandshake;

import me.earth.earthhack.api.module.data.AbstractData;

final class OffhandShakeData extends AbstractData<OffhandShake>
{
    public OffhandShakeData(OffhandShake module)
    {
        super(module);
        descriptions.put(module.amount,
                "How far the offhand item dips down (0 = full shake).");
        descriptions.put(module.speed,
                "Animation recovery speed.");
    }

    @Override
    public int getColor()
    {
        return 0xff40c0ff;
    }

    @Override
    public String getDescription()
    {
        return "Shakes the offhand item when breaking crystals.";
    }
}
