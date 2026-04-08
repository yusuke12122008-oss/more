package me.earth.earthhack.impl.modules.combat.autocrystal;

import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.impl.gui.visibility.PageBuilder;
import me.earth.earthhack.impl.gui.visibility.Visibilities;
import me.earth.earthhack.impl.modules.combat.autocrystal.modes.*;
import me.earth.earthhack.impl.util.helpers.blocks.modes.PlaceSwing;
import me.earth.earthhack.impl.util.helpers.blocks.modes.RayTraceMode;
import me.earth.earthhack.impl.util.helpers.blocks.modes.Rotate;
import me.earth.earthhack.impl.util.minecraft.CooldownBypass;

import java.awt.*;

public class AutoCrystal2 extends AutoCrystal
{
    private final Setting<AC2Pages> ac2Pages =
        register(new EnumSetting<>("Page", AC2Pages.Place));

    public AutoCrystal2()
    {
        super();
        this.setData(new AutoCrystal2Data(this));
        lockAdvancedSettings();
        setupPages();
    }

    @Override
    public String getName()
    {
        return "AutoCrystal2";
    }

    @Override
    protected void onEnable()
    {
        lockAdvancedSettings();
        super.onEnable();
    }

    private void setupPages()
    {
        PageBuilder<AC2Pages> builder =
            new PageBuilder<>(this, ac2Pages);

        builder.addPage(p -> p == AC2Pages.Place,
            place, placeRange, minDamage, placeDelay,
            maxSelfPlace, multiPlace, newVer, newVerEntities);

        builder.addPage(p -> p == AC2Pages.Break,
            attack, breakRange, breakDelay, breakTrace,
            maxSelfBreak, instant, antiWeakness);

        builder.addPage(p -> p == AC2Pages.Rotate,
            rotate, rotateMode, existed);

        builder.addPage(p -> p == AC2Pages.Misc,
            targetRange, range, suicide, multiTask,
            cooldown, antiFriendPop, stopWhenEating);

        builder.addPage(p -> p == AC2Pages.FacePlace,
            holdFacePlace, facePlace, armorPlace);

        builder.addPage(p -> p == AC2Pages.Switch,
            autoSwitch, mainHand, switchBind, switchBack);

        builder.addPage(p -> p == AC2Pages.Render,
            render, boxColor, outLine, fade, renderDamage);

        builder.addPage(p -> p == AC2Pages.SetDead,
            setDead, soundRemove);

        builder.addPage(p -> p == AC2Pages.Obsidian,
            obsidian, helpingBlocks, obbyRotate);

        builder.addPage(p -> p == AC2Pages.Thread,
            multiThread, threadDelay);

        builder.register(Visibilities.VISIBILITY_MANAGER);
    }

    private void lockAdvancedSettings()
    {
        // ===== PLACE =====
        targetMode.setValue(Target.Closest);
        placeTrace.setValue(6.0f);
        slowPlaceDmg.setValue(4.0f);
        slowPlaceDelay.setValue(500);
        override.setValue(false);
        placeSwing.setValue(SwingTime.Post);
        smartTrace.setValue(false);
        placeRangeEyes.setValue(false);
        placeRangeCenter.setValue(true);
        traceWidth.setValue(-1.0);
        fallbackTrace.setValue(true);
        rayTraceBypass.setValue(false);
        forceBypass.setValue(false);
        rayBypassFacePlace.setValue(false);
        rayBypassFallback.setValue(false);
        bypassTicks.setValue(10);
        rbYaw.setValue(180.0f);
        rbPitch.setValue(90.0f);
        bypassRotationTime.setValue(500);
        ignoreNonFull.setValue(false);
        efficientPlacements.setValue(false);
        simulatePlace.setValue(0);

        // ===== BREAK =====
        attackMode.setValue(Attack.Crystal);
        minBreakDamage.setValue(0.5f);
        slowBreakDamage.setValue(3.0f);
        slowBreakDelay.setValue(500);
        asyncCalc.setValue(false);
        alwaysCalc.setValue(false);
        ncpRange.setValue(false);
        placeBreakRange.setValue(SmartRange.None);
        smartTicks.setValue(0);
        negativeTicks.setValue(0);
        smartBreakTrace.setValue(true);
        negativeBreakTrace.setValue(true);
        packets.setValue(1);
        overrideBreak.setValue(false);
        instantAntiWeak.setValue(true);
        efficient.setValue(true);
        manually.setValue(true);
        manualDelay.setValue(500);
        breakSwing.setValue(SwingTime.Post);

        // ===== ROTATION =====
        smoothSpeed.setValue(0.5f);
        endRotations.setValue(250);
        angle.setValue(180.0f);
        placeAngle.setValue(180.0f);
        height.setValue(0.05f);
        placeHeight.setValue(1.0);
        rotationTicks.setValue(0);
        focusRotations.setValue(false);
        focusAngleCalc.setValue(false);
        focusExponent.setValue(0.0);
        focusDiff.setValue(0.0);
        rotationExponent.setValue(0.0);
        minRotDiff.setValue(0.0);
        pingExisted.setValue(false);

        // ===== MISC =====
        pbTrace.setValue(3.0f);
        shield.setValue(false);
        shieldCount.setValue(1);
        shieldMinDamage.setValue(6.0f);
        shieldSelfDamage.setValue(2.0f);
        shieldDelay.setValue(50);
        shieldRange.setValue(10.0f);
        shieldPrioritizeHealth.setValue(false);
        multiPlaceCalc.setValue(true);
        multiPlaceMinDmg.setValue(true);
        countDeadCrystals.setValue(false);
        countDeathTime.setValue(false);
        yCalc.setValue(false);
        dangerSpeed.setValue(false);
        dangerHealth.setValue(0.0f);
        placeCoolDown.setValue(0);
        antiFeetPlace.setValue(false);
        feetBuffer.setValue(5);
        stopWhenMining.setValue(false);
        dangerFacePlace.setValue(false);
        motionCalc.setValue(false);

        // ===== FACEPLACE =====
        minFaceDmg.setValue(2.0f);
        pickAxeHold.setValue(false);
        antiNaked.setValue(false);
        fallBack.setValue(true);
        fallBackDiff.setValue(10.0f);
        fallBackDmg.setValue(3.0f);

        // ===== SWITCH =====
        useAsOffhand.setValue(false);
        instantOffhand.setValue(true);
        pingBypass.setValue(true);
        switchMessage.setValue(false);
        swing.setValue(SwingType.MainHand);
        placeHand.setValue(SwingType.MainHand);
        cooldownBypass.setValue(CooldownBypass.None);
        obsidianBypass.setValue(CooldownBypass.None);
        antiWeaknessBypass.setValue(CooldownBypass.None);
        mineBypass.setValue(CooldownBypass.None);
        obbyHand.setValue(SwingType.MainHand);

        // ===== RENDER =====
        renderTime.setValue(600);
        box.setValue(true);
        indicatorColor.setValue(new Color(190, 5, 5, 255));
        fadeComp.setValue(false);
        fadeTime.setValue(1000);
        realtime.setValue(false);
        slide.setValue(false);
        smoothSlide.setValue(false);
        slideTime.setValue(250.0);
        zoom.setValue(false);
        zoomTime.setValue(100.0);
        zoomOffset.setValue(-0.5);
        multiZoom.setValue(false);
        renderExtrapolation.setValue(false);
        renderMode.setValue(RenderDamage.Normal);

        // ===== ARRAYLIST =====
        arrayInfo.setValue(false);
        showTarget.setValue(true);
        showDelay.setValue(true);
        showSpeed.setValue(true);
        showCPS.setValue(true);

        // ===== SETDEAD =====
        instantSetDead.setValue(false);
        pseudoSetDead.setValue(false);
        simulateExplosion.setValue(false);
        useSafeDeathTime.setValue(false);
        safeDeathTime.setValue(0);
        deathTime.setValue(0);

        // ===== OBSIDIAN =====
        basePlaceOnly.setValue(false);
        obbySwitch.setValue(false);
        obbyDelay.setValue(500);
        obbyCalc.setValue(500);
        obbyMinDmg.setValue(7.0f);
        terrainCalc.setValue(true);
        obbySafety.setValue(false);
        obbyTrace.setValue(RayTraceMode.Fast);
        obbyTerrain.setValue(true);
        obbyPreSelf.setValue(true);
        fastObby.setValue(0);
        maxDiff.setValue(1);
        maxDmgDiff.setValue(0.0);
        setState.setValue(false);
        obbySwing.setValue(PlaceSwing.Once);
        obbyFallback.setValue(false);

        // ===== LIQUIDS (all OFF) =====
        interact.setValue(false);
        inside.setValue(false);
        lava.setValue(false);
        water.setValue(false);
        liquidObby.setValue(false);
        liquidRayTrace.setValue(false);
        liqDelay.setValue(500);
        liqRotate.setValue(Rotate.None);
        pickaxeOnly.setValue(false);
        interruptSpeedmine.setValue(false);
        setAir.setValue(true);
        absorb.setValue(false);
        requireOnGround.setValue(true);
        ignoreLavaItems.setValue(false);
        sponges.setValue(false);

        // ===== ANTITOTEM (all OFF) =====
        antiTotem.setValue(false);
        totemHealth.setValue(1.5f);
        minTotemOffset.setValue(0.5f);
        maxTotemOffset.setValue(2.0f);
        popDamage.setValue(12.0f);
        totemSync.setValue(true);
        forceAntiTotem.setValue(false);
        forceSlow.setValue(false);
        syncForce.setValue(true);
        dangerForce.setValue(false);
        forcePlaceConfirm.setValue(100);
        forceBreakConfirm.setValue(100);
        attempts.setValue(500);

        // ===== DAMAGESYNC (all OFF) =====
        damageSync.setValue(false);
        preSynCheck.setValue(false);
        discreteSync.setValue(false);
        dangerSync.setValue(false);
        placeConfirm.setValue(250);
        breakConfirm.setValue(250);
        syncDelay.setValue(500);
        surroundSync.setValue(true);

        // ===== EXTRAPOLATION (all 0/OFF) =====
        extrapol.setValue(0);
        bExtrapol.setValue(0);
        blockExtrapol.setValue(0);
        blockExtraMode.setValue(BlockExtrapolationMode.Pessimistic);
        doubleExtraCheck.setValue(true);
        avgPlaceDamage.setValue(false);
        placeExtraWeight.setValue(1.0);
        placeNormalWeight.setValue(1.0);
        avgBreakExtra.setValue(false);
        breakExtraWeight.setValue(1.0);
        breakNormalWeight.setValue(1.0);
        gravityExtrapolation.setValue(true);
        gravityFactor.setValue(1.0);
        yPlusFactor.setValue(1.0);
        yMinusFactor.setValue(1.0);
        selfExtrapolation.setValue(false);

        // ===== ID-PREDICT (all OFF) =====
        idPredict.setValue(false);
        idOffset.setValue(1);
        idDelay.setValue(0);
        idPackets.setValue(1);
        godAntiTotem.setValue(false);
        holdingCheck.setValue(true);
        toolCheck.setValue(true);
        godSwing.setValue(PlaceSwing.Once);

        // ===== EFFICIENCY (all OFF) =====
        preCalc.setValue(PreCalc.None);
        preCalcExtra.setValue(ExtrapolationType.Place);
        preCalcDamage.setValue(15.0f);

        // ===== MULTITHREAD (lock extras) =====
        rotationThread.setValue(RotationThread.Predict);
        gameloop.setValue(true);
        motionThread.setValue(true);
    }
}
