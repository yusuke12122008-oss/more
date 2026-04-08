package me.earth.earthhack.impl.modules.combat.autocrystal;

import me.earth.earthhack.api.module.data.DefaultData;

public class AutoCrystal2Data extends DefaultData<AutoCrystal2>
{
    public AutoCrystal2Data(AutoCrystal2 module)
    {
        super(module);

        // ---- Place ----
        register(module.place, "Toggle crystal placement on/off.");
        register(module.placeRange, "Range to the block you want to place on.");
        register(module.minDamage, "Minimum damage a crystal should deal to an enemy.");
        register(module.placeDelay, "Delay in ms between each crystal placement.");
        register(module.maxSelfPlace, "Maximum damage a placed crystal can deal to you.");
        register(module.multiPlace, "Max number of valid crystals before placing new ones.");
        register(module.newVer, "1.13+ mechanics: place in 1-block-high spaces.");
        register(module.newVerEntities, "Allow placing crystals underneath entities.");

        // ---- Break ----
        register(module.attack, "Toggle crystal breaking on/off.");
        register(module.breakRange, "Only crystals within this range will be attacked.");
        register(module.breakDelay, "Delay in ms between attacks.");
        register(module.breakTrace, "Attack through walls within this range.");
        register(module.maxSelfBreak, "Maximum self-damage when breaking crystals.");
        register(module.instant, "Attack crystals the instant they spawn.");
        register(module.antiWeakness, "Counter weakness effect: None or Switch.");

        // ---- Rotate ----
        register(module.rotate, "Rotation mode: None, Break, Place, or All.");
        register(module.rotateMode, "Normal or Smooth rotation style.");
        register(module.existed, "Time in ms a crystal must exist before attacking.");

        // ---- Misc ----
        register(module.targetRange, "Only target players within this range.");
        register(module.range, "Max distance from target to crystal.");
        register(module.suicide, "Allow self-damage to hurt the target.");
        register(module.multiTask, "Allow placing and breaking in the same tick.");
        register(module.cooldown, "Switch cooldown in ms.");
        register(module.antiFriendPop, "Prevent popping friends: None/Place/Break/All.");
        register(module.stopWhenEating, "Pause while eating.");

        // ---- FacePlace ----
        register(module.holdFacePlace, "Hold left-click to faceplace.");
        register(module.facePlace, "Faceplace when target HP is below this.");
        register(module.armorPlace, "Faceplace when target armor durability is below this %.");

        // ---- Switch ----
        register(module.autoSwitch, "Auto-switch mode: None, Bind, or Always.");
        register(module.mainHand, "Use main hand for crystals.");
        register(module.switchBind, "Keybind for switching to crystals.");
        register(module.switchBack, "Switch back after placing.");

        // ---- Render ----
        register(module.render, "Toggle ESP rendering.");
        register(module.boxColor, "Color of the rendered box.");
        register(module.outLine, "Color of the box outline.");
        register(module.fade, "Fade animation on position change.");
        register(module.renderDamage, "Show damage numbers: None/Inside/OnTop.");

        // ---- SetDead ----
        register(module.setDead, "Remove crystals client-side after attacking. Use on reliable servers only.");
        register(module.soundRemove, "Detect explosions via sound packets (faster).");

        // ---- Obsidian ----
        register(module.obsidian, "Auto-place obsidian for crystal positions.");
        register(module.helpingBlocks, "Max supporting blocks for obsidian.");
        register(module.obbyRotate, "Rotation for obsidian: None/Normal/Packet.");

        // ---- Thread ----
        register(module.multiThread, "Run calculations on a separate thread (better FPS).");
        register(module.threadDelay, "Delay between calculations (lower = more CPU).");
    }

    @Override
    public String getDescription()
    {
        return "Simplified AutoCrystal with 44 essential settings.";
    }
}
