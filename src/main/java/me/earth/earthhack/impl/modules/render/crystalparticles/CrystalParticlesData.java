package me.earth.earthhack.impl.modules.render.crystalparticles;

import me.earth.earthhack.api.module.data.AbstractData;

final class CrystalParticlesData extends AbstractData<CrystalParticles>
{
    public CrystalParticlesData(CrystalParticles module)
    {
        super(module);
        descriptions.put(module.critParticles,
                "Spawns critical hit particles on crystal attacks.");
        descriptions.put(module.magicParticles,
                "Spawns magic (enchantment) particles on crystal attacks.");
    }

    @Override
    public int getColor()
    {
        return 0xff8040ff;
    }

    @Override
    public String getDescription()
    {
        return "Spawns particles when attacking an Ender Crystal.";
    }
}
