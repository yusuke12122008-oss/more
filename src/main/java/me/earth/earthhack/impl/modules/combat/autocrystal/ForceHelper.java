package me.earth.earthhack.impl.modules.combat.autocrystal;

import me.earth.earthhack.impl.util.math.StopWatch;
import net.minecraft.util.math.BlockPos;

/**
 * Tracks forced anti-totem positions for a short window so calculations can
 * prioritize synchronized break/place sequences.
 */
public class ForceHelper
{
    private static final int FORCE_TIME = 500;

    private final StopWatch timer = new StopWatch();
    private final AutoCrystal module;

    private BlockPos pos;

    public ForceHelper(AutoCrystal module)
    {
        this.module = module;
    }

    public void setSync(BlockPos pos, boolean newerVersion)
    {
        this.pos = pos;
        this.timer.reset();
    }

    public BlockPos getPos()
    {
        if (!isForcing(false))
        {
            pos = null;
        }

        return pos;
    }

    public boolean isForcing(boolean syncForce)
    {
        if (pos == null)
        {
            return false;
        }

        if (!module.forceAntiTotem.getValue())
        {
            return false;
        }

        if (!syncForce)
        {
            return timer.passed(FORCE_TIME);
        }

        return timer.passed(module.syncDelay.getValue() + FORCE_TIME);
    }
}
