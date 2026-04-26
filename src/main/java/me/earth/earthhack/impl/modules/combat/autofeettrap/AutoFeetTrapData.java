package me.earth.earthhack.impl.modules.combat.autofeettrap;

import me.earth.earthhack.impl.util.client.ModuleData;

final class AutoFeetTrapData extends ModuleData<AutoFeetTrap>
{
    AutoFeetTrapData(AutoFeetTrap module)
    {
        super(module);
        register(module.mode, "NORMAL places around 4 sides. FULL adds diagonal placements.");
        register(module.blockMode, "Chooses allowed hotbar block type for placing.");
        register(module.blocksPerTick, "Maximum blocks to place per tick cycle.");
        register(module.delayTicks, "Tick delay between each placement cycle.");
        register(module.onlyOnGround, "Only runs while standing on ground.");
        register(module.disableOnJump, "Disables if upward jump movement is detected.");
        register(module.disableOnMove, "Disables if you move away from your start block.");
        register(module.antiStep, "Also places a second layer to reduce step-ins.");
        register(module.support, "Adds support blocks when direct placement has no neighbor.");
        register(module.rotate, "Rotates before placing blocks.");
        register(module.centerPlayer, "Applies soft movement toward block center.");
    }

    @Override
    public int getColor()
    {
        return 0xff8fdaff;
    }

    @Override
    public String getDescription()
    {
        return "Places blocks around your feet like surround for NCP-friendly trapping.";
    }
}
