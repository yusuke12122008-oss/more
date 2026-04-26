package me.earth.earthhack.impl.modules.combat.crystalaura;

import me.earth.earthhack.impl.util.client.ModuleData;

final class CrystalAuraData extends ModuleData<CrystalAura>
{
    CrystalAuraData(CrystalAura module)
    {
        super(module);
        register(module.placeRange, "Maximum range for crystal placement scans.");
        register(module.breakRange, "Maximum range for crystal breaking.");
        register(module.wallRange, "Range for through-wall crystal breaking.");
        register(module.placeDelay, "Delay in milliseconds between placement attempts.");
        register(module.breakDelay, "Delay in milliseconds between break attempts.");
        register(module.minDamage, "Minimum target damage required for actions.");
        register(module.maxSelfDamage, "Maximum allowed self-damage.");
        register(module.rotate, "Rotates before placing or breaking crystals.");
        register(module.predictSpawn, "Attempts to instantly break newly spawned crystals.");
        register(module.antiWeakness, "Switches to a weakness-safe weapon before breaking.");
        register(module.antiSuicide, "Prevents actions that would likely kill you.");
        register(module.raytrace, "Requires visibility checks for place and break paths.");
        register(module.facePlace, "Lowers damage threshold on low-health or low-armor targets.");
        register(module.facePlaceHealth, "Health threshold used for face place behavior.");
        register(module.armorBreaker, "Armor percent threshold used for face place behavior.");
        register(module.multiThread, "Runs aura logic asynchronously.");
        register(module.setDead, "Marks attacked crystals as dead through SetDeadManager.");
        register(module.pseudoDead, "Marks attacked crystals as pseudo-dead clientside.");
        register(module.soundRemove, "Tracks explosions from sound packets.");
        register(module.deathTime, "Death-time used when evaluating set-dead crystals.");
        register(module.useSafeDeathTime, "Uses SafeDeathTime in low-health situations.");
        register(module.safeDeathTime, "Alternative death-time used in danger states.");
        register(module.sequential, "Waits for expected sequence confirmation after placing.");
        register(module.seqTime, "Maximum wait time for sequence confirmation.");
        register(module.endSequenceOnSpawn, "Ends sequence when matching spawn packet arrives.");
        register(module.endSequenceOnBreak, "Ends sequence when matching break packet arrives.");
        register(module.endSequenceOnExplosion, "Ends sequence when matching explosion sound arrives.");
        register(module.antiPlaceFail, "Clears stale sequence when place confirmation fails.");
        register(module.autoSwitch, "Controls how crystal auto-switching is handled.");
        register(module.switchBind, "Bind used by AutoSwitch=Bind mode.");
        register(module.switchBack, "Switches back to the previous slot after actions.");
        register(module.swapDelay, "Delay after swaps before new aura actions are allowed.");
        register(module.targetMode, "Chooses target priority by distance or health.");
    }

    @Override
    public int getColor()
    {
        return 0xffd26fff;
    }

    @Override
    public String getDescription()
    {
        return "Automatically places and breaks end crystals.";
    }
}
