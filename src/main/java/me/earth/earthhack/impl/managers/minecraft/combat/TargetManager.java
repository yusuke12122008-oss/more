package me.earth.earthhack.impl.managers.minecraft.combat;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.modules.Caches;

import me.earth.earthhack.impl.modules.combat.killaura.KillAura;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class TargetManager
{

    private static final ModuleCache<KillAura> KILL_AURA =
            Caches.getModule(KillAura.class);


    public Entity getKillAura()
    {
        return KILL_AURA.returnIfPresent(KillAura::getTarget, null);
    }

    public EntityPlayer getAutoTrap()
    {
        return null;
    }

    public EntityPlayer getAutoCrystal()
    {
        return null;
    }

    public Entity getCrystal()
    {
        return null;
    }

}
