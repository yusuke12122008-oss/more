package me.earth.earthhack.impl.modules.combat.autocrystal;

import me.earth.earthhack.api.event.events.Stage;
import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.event.events.network.MotionUpdateEvent;
import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.combat.util.CombatCoordinator;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.minecraft.DamageUtil;
import me.earth.earthhack.impl.util.minecraft.InventoryUtil;
import me.earth.earthhack.impl.util.minecraft.entity.EntityUtil;
import me.earth.earthhack.impl.util.network.PacketUtil;
import me.earth.earthhack.impl.util.render.Interpolation;
import me.earth.earthhack.impl.util.render.RenderUtil;
import me.earth.earthhack.impl.util.thread.Locks;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.server.SPacketDestroyEntities;
import net.minecraft.network.play.server.SPacketSpawnObject;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AutoCrystal extends Module
{
    protected final Setting<Boolean> arrayInfo =
        register(new BooleanSetting("ArrayInfo", true));

    protected final Setting<Boolean> showTarget =
        register(new BooleanSetting("ShowTarget", true));

    protected final Setting<Boolean> showDamage =
        register(new BooleanSetting("ShowDamage", true));

    protected final Setting<Boolean> showCPS =
        register(new BooleanSetting("ShowCPS", true));

    protected final Setting<TargetMode> targetMode =
        register(new EnumSetting<>("Target", TargetMode.DAMAGE));

    protected final Setting<Float> targetRange =
        register(new NumberSetting<>("TargetRange", 10.0f, 1.0f, 20.0f));

    protected final Setting<Boolean> place =
        register(new BooleanSetting("Place", true));

    protected final Setting<Boolean> breakCrystals =
        register(new BooleanSetting("Break", true));

    protected final Setting<Float> placeRange =
        register(new NumberSetting<>("PlaceRange", 5.5f, 1.0f, 6.0f));

    protected final Setting<Float> breakRange =
        register(new NumberSetting<>("BreakRange", 5.8f, 1.0f, 6.0f));

    protected final Setting<Float> placeWallsRange =
        register(new NumberSetting<>("PlaceWalls", 3.0f, 0.0f, 6.0f));

    protected final Setting<Float> breakWallsRange =
        register(new NumberSetting<>("BreakWalls", 3.0f, 0.0f, 6.0f));

    protected final Setting<Integer> placeDelay =
        register(new NumberSetting<>("PlaceDelay", 50, 0, 1000));

    protected final Setting<Integer> breakDelay =
        register(new NumberSetting<>("BreakDelay", 50, 0, 1000));

    protected final Setting<Integer> slowPlaceDelay =
        register(new NumberSetting<>("SlowPlaceDelay", 350, 0, 1000));

    protected final Setting<Integer> slowBreakDelay =
        register(new NumberSetting<>("SlowBreakDelay", 350, 0, 1000));

    protected final Setting<Float> slowDamage =
        register(new NumberSetting<>("SlowDamage", 3.0f, 0.0f, 20.0f));

    protected final Setting<Integer> multiPlace =
        register(new NumberSetting<>("MultiPlace", 1, 1, 5));

    protected final Setting<Integer> packets =
        register(new NumberSetting<>("Packets", 1, 1, 5));

    protected final Setting<Float> minDamage =
        register(new NumberSetting<>("MinDamage", 4.0f, 0.0f, 36.0f));

    protected final Setting<Float> maxSelfDamage =
        register(new NumberSetting<>("MaxSelfDmg", 8.0f, 0.0f, 36.0f));

    protected final Setting<Boolean> antiSuicide =
        register(new BooleanSetting("AntiSuicide", true));

    protected final Setting<Boolean> facePlace =
        register(new BooleanSetting("FacePlace", true));

    protected final Setting<Float> facePlaceHealth =
        register(new NumberSetting<>("FP-Health", 10.0f, 0.0f, 36.0f));

    protected final Setting<Float> facePlaceDamage =
        register(new NumberSetting<>("FP-Damage", 2.0f, 0.0f, 10.0f));

    protected final Setting<Boolean> armorPlace =
        register(new BooleanSetting("ArmorPlace", true));

    protected final Setting<Float> armorPercent =
        register(new NumberSetting<>("Armor%", 20.0f, 0.0f, 100.0f));

    protected final Setting<Boolean> antiNaked =
        register(new BooleanSetting("AntiNaked", false));

    protected final Setting<Boolean> antiFriendPop =
        register(new BooleanSetting("AntiFriendPop", true));

    protected final Setting<Float> maxFriendDamage =
        register(new NumberSetting<>("MaxFriendDmg", 2.0f, 0.0f, 20.0f));

    protected final Setting<Integer> lethalCrystals =
        register(new NumberSetting<>("LethalCrystals", 0, 0, 5));

    protected final Setting<Boolean> onePointTwelve =
        register(new BooleanSetting("1.12", true));

    protected final Setting<Boolean> antiFeetPlace =
        register(new BooleanSetting("AntiFeetPlace", false));

    protected final Setting<Float> antiFeetDamage =
        register(new NumberSetting<>("AFP-Damage", 2.0f, 0.0f, 10.0f));

    protected final Setting<Integer> ticksExisted =
        register(new NumberSetting<>("TicksExisted", 0, 0, 20));

    protected final Setting<Boolean> predict =
        register(new BooleanSetting("Predict", true));

    protected final Setting<Boolean> inhibit =
        register(new BooleanSetting("Inhibit", true));

    protected final Setting<Boolean> setDead =
        register(new BooleanSetting("SetDead", false));

    protected final Setting<Boolean> simulateExplosion =
        register(new BooleanSetting("SimExplosion", false));

    protected final Setting<AutoSwitch> autoSwitch =
        register(new EnumSetting<>("AutoSwitch", AutoSwitch.SILENT));

    protected final Setting<Boolean> noGapSwitch =
        register(new BooleanSetting("NoGapSwitch", true));

    protected final Setting<AntiWeakness> antiWeakness =
        register(new EnumSetting<>("AntiWeakness", AntiWeakness.SILENT));

    protected final Setting<MultiTask> multiTask =
        register(new EnumSetting<>("MultiTask", MultiTask.SOFT));

    protected final Setting<Boolean> stopWhenMining =
        register(new BooleanSetting("StopMining", false));

    protected final Setting<Boolean> strictDirection =
        register(new BooleanSetting("StrictDirection", false));

    protected final Setting<Boolean> strictFallback =
        register(new BooleanSetting("StrictFallback", true));

    protected final Setting<Boolean> smartTrace =
        register(new BooleanSetting("SmartTrace", true));

    protected final Setting<Boolean> rayBypass =
        register(new BooleanSetting("RayBypass", false));

    protected final Setting<Boolean> rotate =
        register(new BooleanSetting("Rotate", true));

    protected final Setting<RotationMode> rotationMode =
        register(new EnumSetting<>("RotationMode", RotationMode.MOTION));

    protected final Setting<Boolean> smoothRotate =
        register(new BooleanSetting("SmoothRotate", false));

    protected final Setting<Float> smoothSpeed =
        register(new NumberSetting<>("SmoothSpeed", 0.5f, 0.05f, 1.0f));

    protected final Setting<Boolean> focusRotation =
        register(new BooleanSetting("FocusRotation", false));

    protected final Setting<Sequence> sequence =
        register(new EnumSetting<>("Sequence", Sequence.SOFT));

    protected final Setting<Integer> cpsLimit =
        register(new NumberSetting<>("CPSLimit", 0, 0, 20));

    protected final Setting<HandSwing> handSwing =
        register(new EnumSetting<>("HandSwing", HandSwing.PACKET));

    protected final Setting<SwingHand> breakSwingHand =
        register(new EnumSetting<>("BreakHand", SwingHand.MAINHAND));

    protected final Setting<SwingHand> placeSwingHand =
        register(new EnumSetting<>("PlaceHand", SwingHand.AUTO));

    protected final Setting<Priority> priority =
        register(new EnumSetting<>("Priority", Priority.BALANCED));

    protected final Setting<Boolean> pauseForFeetTrap =
        register(new BooleanSetting("PauseFeetTrap", true));

    protected final Setting<Boolean> avoidFeetTargets =
        register(new BooleanSetting("AvoidFeetTargets", true));

    protected final Setting<Boolean> breakForFeetTrap =
        register(new BooleanSetting("BreakFeetTrap", true));

    protected final Setting<Boolean> render =
        register(new BooleanSetting("Render", true));

    protected final Setting<Boolean> renderDamage =
        register(new BooleanSetting("DamageText", true));

    protected final Setting<RenderMode> renderMode =
        register(new EnumSetting<>("RenderMode", RenderMode.FADE));

    protected final Setting<Integer> renderTime =
        register(new NumberSetting<>("RenderTime", 600, 0, 5000));

    protected final Setting<Color> fill =
        register(new ColorSetting("Fill", new Color(255, 255, 255, 70)));

    protected final Setting<Color> line =
        register(new ColorSetting("Line", new Color(255, 255, 255, 220)));

    private final Map<Integer, Long> attackedCrystals = new ConcurrentHashMap<>();
    private final Map<BlockPos, Long> placedCrystals = new ConcurrentHashMap<>();
    private final Map<BlockPos, CrystalResult> damageCache = new ConcurrentHashMap<>();
    private final Map<BlockPos, RenderEntry> renderMap = new ConcurrentHashMap<>();
    private final LinkedList<Long> cps = new LinkedList<>();

    private EntityPlayer target;
    private EntityEnderCrystal breakCrystal;
    private CrystalResult placeResult;
    private BlockPos renderPos;
    private float renderDmg;
    private float[] rotations;

    private long lastPlace;
    private long lastBreak;
    private long lastSwitch;
    private float lastYaw;
    private float lastPitch;

    public AutoCrystal()
    {
        super("AutoCrystal", Category.Combat);

        this.listeners.add(new ListenerMotion(this));
        this.listeners.add(new ListenerSpawnObject(this));
        this.listeners.add(new ListenerDestroyEntities(this));
        this.listeners.add(new ListenerRender(this));

        this.setData(new SimpleData(this,
            "Standalone 1.12.2 AutoCrystal with CatAura-inspired logic.",
            0xffffffff));
    }

    @Override
    protected void onEnable()
    {
        attackedCrystals.clear();
        placedCrystals.clear();
        damageCache.clear();
        renderMap.clear();
        cps.clear();

        target = null;
        breakCrystal = null;
        placeResult = null;
        renderPos = null;
        rotations = null;
        renderDmg = 0.0f;

        lastPlace = 0L;
        lastBreak = 0L;
        lastSwitch = 0L;
        lastYaw = mc.player == null ? 0.0f : mc.player.rotationYaw;
        lastPitch = mc.player == null ? 0.0f : mc.player.rotationPitch;

        CombatCoordinator.setAutoCrystalActive(true);
    }

    @Override
    protected void onDisable()
    {
        attackedCrystals.clear();
        placedCrystals.clear();
        damageCache.clear();
        renderMap.clear();
        cps.clear();

        target = null;
        breakCrystal = null;
        placeResult = null;
        renderPos = null;
        rotations = null;

        CombatCoordinator.setAutoCrystalActive(false);
    }

    @Override
    public String getDisplayInfo()
    {
        if (!arrayInfo.getValue())
        {
            return null;
        }

        StringBuilder builder = new StringBuilder();

        if (target != null)
        {
            builder.append(target.getName());
        }

        if (showDamage.getValue() && renderDmg > 0.0f)
        {
            append(builder, String.format("%.1f", renderDmg));
        }

        if (showCPS.getValue())
        {
            append(builder, getCps() + "cps");
        }

        return builder.length() == 0 ? null : builder.toString();
    }

    private void append(StringBuilder builder, String text)
    {
        if (builder.length() > 0)
        {
            builder.append(" ");
        }

        builder.append(text);
    }

    private void onMotion(MotionUpdateEvent event)
    {
        if (mc.player == null || mc.world == null)
        {
            return;
        }

        if (event.getStage() == Stage.PRE)
        {
            prepare();

            if (rotate.getValue()
                && rotationMode.getValue() == RotationMode.MOTION
                && rotations != null)
            {
                Managers.ROTATION.setBlocking(true);
                event.setYaw(rotations[0]);
                event.setPitch(rotations[1]);
            }
        }
        else
        {
            if (sequence.getValue() == Sequence.STRONG)
            {
                doBreak();
                doPlace();
            }
            else
            {
                doPlace();
                doBreak();
            }

            Managers.ROTATION.setBlocking(false);
        }
    }

    private void prepare()
    {
        cleanCaches();

        target = findTarget();
        breakCrystal = null;
        placeResult = null;
        rotations = null;

        if (target == null || isMultiTaskBlocked())
        {
            renderPos = null;
            return;
        }

        if (stopWhenMining.getValue() && isMining())
        {
            return;
        }

        if (breakCrystals.getValue())
        {
            breakCrystal = findBestCrystal(target);
        }

        if (place.getValue())
        {
            placeResult = findBestPlace(target);
        }

        if (breakCrystal != null)
        {
            rotations = calculateRotations(breakCrystal.posX,
                                           breakCrystal.posY + breakCrystal.height * 0.5,
                                           breakCrystal.posZ);
        }
        else if (placeResult != null)
        {
            rotations = calculateRotations(placeResult.pos.getX() + 0.5,
                                           placeResult.pos.getY() + 1.0,
                                           placeResult.pos.getZ() + 0.5);
        }

        if (rotations != null && smoothRotate.getValue())
        {
            rotations = smooth(rotations[0], rotations[1]);
        }

        if (placeResult != null)
        {
            renderPos = placeResult.pos;
            renderDmg = placeResult.targetDamage;
        }
    }

    private void doBreak()
    {
        if (breakCrystal == null)
        {
            return;
        }

        int delay = getBreakDelay(breakCrystal, target);

        if (!passed(lastBreak, delay))
        {
            return;
        }

        if (cpsLimit.getValue() > 0 && getCps() >= cpsLimit.getValue())
        {
            return;
        }

        if (inhibit.getValue()
            && attackedCrystals.containsKey(breakCrystal.getEntityId()))
        {
            return;
        }

        if (mc.player.getDistanceSq(breakCrystal) > square(breakRange.getValue()))
        {
            return;
        }

        int oldSlot = mc.player.inventory.currentItem;
        boolean switched = false;

        if (antiWeakness.getValue() != AntiWeakness.NONE
            && !DamageUtil.canBreakWeakness(true))
        {
            int weapon = findWeaponSlot();

            if (weapon == -1)
            {
                return;
            }

            if (weapon != oldSlot)
            {
                switchTo(weapon);
                switched = true;
            }
        }

        for (int i = 0; i < packets.getValue(); i++)
        {
            attackCrystal(breakCrystal);
        }

        if (switched && antiWeakness.getValue() == AntiWeakness.SILENT)
        {
            switchTo(oldSlot);
        }

        if (setDead.getValue())
        {
            mc.world.removeEntityFromWorld(breakCrystal.getEntityId());
        }

        if (simulateExplosion.getValue())
        {
            mc.world.createExplosion(mc.player,
                breakCrystal.posX,
                breakCrystal.posY,
                breakCrystal.posZ,
                6.0f,
                false);
        }

        attackedCrystals.put(breakCrystal.getEntityId(), System.currentTimeMillis());
        CombatCoordinator.markCrystalAttacked(breakCrystal.getEntityId());
        cps.add(System.currentTimeMillis());
        lastBreak = System.currentTimeMillis();
    }

    private int getBreakDelay(EntityEnderCrystal crystal, EntityPlayer target)
    {
        float damage = DamageUtil.calculate(crystal, target);
        return damage <= slowDamage.getValue()
            ? slowBreakDelay.getValue()
            : breakDelay.getValue();
    }

    private void attackCrystal(EntityEnderCrystal crystal)
    {
        if (rotate.getValue() && rotationMode.getValue() == RotationMode.PACKET)
        {
            float[] r = calculateRotations(crystal.posX,
                                           crystal.posY + crystal.height * 0.5,
                                           crystal.posZ);
            PacketUtil.doRotation(r[0], r[1], mc.player.onGround);
        }

        mc.player.connection.sendPacket(new CPacketUseEntity(crystal));
        swing(getSwingHand(breakSwingHand.getValue(), EnumHand.MAIN_HAND));
    }

    private void doPlace()
    {
        if (placeResult == null || shouldPauseForFeetTrap())
        {
            return;
        }

        int delay = placeResult.targetDamage <= slowDamage.getValue()
            ? slowPlaceDelay.getValue()
            : placeDelay.getValue();

        if (!passed(lastPlace, delay))
        {
            return;
        }

        Locks.acquire(Locks.PLACE_SWITCH_LOCK, () ->
        {
            CombatCoordinator.setAutoCrystalPlacing(true);

            try
            {
                doPlaceLocked();
            }
            finally
            {
                CombatCoordinator.setAutoCrystalPlacing(false);
            }
        });
    }

    private void doPlaceLocked()
    {
        int crystalSlot = InventoryUtil.findHotbarItem(Items.END_CRYSTAL);
        boolean offhand = mc.player.getHeldItemOffhand().getItem() == Items.END_CRYSTAL;

        if (crystalSlot == -1 && !offhand)
        {
            return;
        }

        int oldSlot = mc.player.inventory.currentItem;
        boolean switched = false;

        if (!offhand && mc.player.getHeldItemMainhand().getItem() != Items.END_CRYSTAL)
        {
            if (autoSwitch.getValue() == AutoSwitch.NONE)
            {
                return;
            }

            if (noGapSwitch.getValue() && mc.player.isHandActive())
            {
                return;
            }

            switchTo(crystalSlot);
            switched = true;
        }

        List<CrystalResult> places = findBestPlaces(target, multiPlace.getValue());
        Set<BlockPos> sharedTargets = new HashSet<>();
        int placed = 0;

        for (CrystalResult result : places)
        {
            if (placed >= multiPlace.getValue())
            {
                break;
            }

            if (avoidFeetTargets.getValue()
                && CombatCoordinator.intersectsFeetTargets(result.pos))
            {
                continue;
            }

            PlaceData data = getPlaceData(result.pos);

            if (data == null)
            {
                continue;
            }

            EnumHand hand = offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;

            if (rotate.getValue() && rotationMode.getValue() == RotationMode.PACKET)
            {
                float[] r = calculateRotations(result.pos.getX() + 0.5,
                                               result.pos.getY() + 1.0,
                                               result.pos.getZ() + 0.5);
                PacketUtil.doRotation(r[0], r[1], mc.player.onGround);
            }

            PacketUtil.place(data.base, data.face, hand, data.hitX, data.hitY, data.hitZ);
            swing(getSwingHand(placeSwingHand.getValue(), hand));

            long now = System.currentTimeMillis();
            placedCrystals.put(result.pos, now);
            renderMap.put(result.pos, new RenderEntry(result.pos, result.targetDamage, now));
            sharedTargets.add(result.pos);
            renderPos = result.pos;
            renderDmg = result.targetDamage;
            placed++;
        }

        CombatCoordinator.updateCrystalPlaceTargets(sharedTargets);

        if (placed > 0)
        {
            lastPlace = System.currentTimeMillis();
        }

        if (switched && autoSwitch.getValue() == AutoSwitch.SILENT)
        {
            switchTo(oldSlot);
        }
    }

    private boolean shouldPauseForFeetTrap()
    {
        if (!pauseForFeetTrap.getValue()
            || !CombatCoordinator.isFeetTrapActive())
        {
            return false;
        }

        if (priority.getValue() == Priority.CRYSTAL)
        {
            return false;
        }

        if (priority.getValue() == Priority.FEET_TRAP)
        {
            return true;
        }

        return CombatCoordinator.isFeetTrapPlacing()
            || placeResult != null
            && CombatCoordinator.intersectsFeetTargets(placeResult.pos);
    }

    private EntityPlayer findTarget()
    {
        EntityPlayer best = null;
        double bestValue = targetMode.getValue() == TargetMode.RANGE
            ? Double.MAX_VALUE
            : -1.0;

        for (EntityPlayer player : mc.world.playerEntities)
        {
            if (player == null
                || player == mc.player
                || EntityUtil.isDead(player)
                || Managers.FRIENDS.contains(player)
                || mc.player.getDistanceSq(player) > square(targetRange.getValue()))
            {
                continue;
            }

            if (antiNaked.getValue() && isNaked(player))
            {
                continue;
            }

            if (targetMode.getValue() == TargetMode.RANGE)
            {
                double dist = mc.player.getDistanceSq(player);

                if (dist < bestValue)
                {
                    bestValue = dist;
                    best = player;
                }
            }
            else if (targetMode.getValue() == TargetMode.HEALTH)
            {
                double health = player.getHealth() + player.getAbsorptionAmount();

                if (best == null || health < bestValue || bestValue < 0.0)
                {
                    bestValue = health;
                    best = player;
                }
            }
            else
            {
                CrystalResult result = findBestPlace(player);
                float damage = result == null ? 0.0f : result.targetDamage;

                if (damage > bestValue)
                {
                    bestValue = damage;
                    best = player;
                }
            }
        }

        return best;
    }

    private EntityEnderCrystal findBestCrystal(EntityPlayer target)
    {
        EntityEnderCrystal best = null;
        float bestDamage = 0.0f;
        float bestRotDiff = Float.MAX_VALUE;

        for (Entity entity : mc.world.loadedEntityList)
        {
            if (!(entity instanceof EntityEnderCrystal))
            {
                continue;
            }

            EntityEnderCrystal crystal = (EntityEnderCrystal) entity;

            if (EntityUtil.isDead(crystal)
                || crystal.ticksExisted < ticksExisted.getValue()
                || mc.player.getDistanceSq(crystal) > square(breakRange.getValue()))
            {
                continue;
            }

            if (breakForFeetTrap.getValue()
                && CombatCoordinator.isFeetTrapActive()
                && intersectsAnyFeetTarget(crystal))
            {
                return crystal;
            }

            if (!canSeeOrWallRange(crystal.getPositionVector(), breakWallsRange.getValue()))
            {
                continue;
            }

            float targetDamage = DamageUtil.calculate(crystal, target);
            float selfDamage = DamageUtil.calculate(crystal, mc.player);

            if (!isDamageValid(target, targetDamage, selfDamage, false, crystal.getPosition()))
            {
                continue;
            }

            float rotDiff = getRotationDiff(crystal.posX,
                                            crystal.posY + crystal.height * 0.5,
                                            crystal.posZ);

            if (targetDamage > bestDamage
                || focusRotation.getValue()
                && targetDamage >= bestDamage - 1.0f
                && rotDiff < bestRotDiff)
            {
                bestDamage = targetDamage;
                bestRotDiff = rotDiff;
                best = crystal;
            }
        }

        return best;
    }

    private boolean intersectsAnyFeetTarget(EntityEnderCrystal crystal)
    {
        AxisAlignedBB bb = crystal.getEntityBoundingBox();

        for (BlockPos pos : CombatCoordinator.getFeetTargets())
        {
            if (bb.intersects(new AxisAlignedBB(pos)))
            {
                return true;
            }
        }

        return false;
    }

    private CrystalResult findBestPlace(EntityPlayer target)
    {
        List<CrystalResult> results = findBestPlaces(target, 1);
        return results.isEmpty() ? null : results.get(0);
    }

    private List<CrystalResult> findBestPlaces(EntityPlayer target, int amount)
    {
        List<BlockPos> sphere = getSphere(getPlayerPos(), MathHelper.ceil(placeRange.getValue()));
        List<CrystalResult> results = new ArrayList<>();

        for (BlockPos pos : sphere)
        {
            if (!canPlaceCrystal(pos))
            {
                continue;
            }

            if (avoidFeetTargets.getValue()
                && CombatCoordinator.intersectsFeetTargets(pos))
            {
                continue;
            }

            if (mc.player.getDistanceSq(pos) > square(placeRange.getValue()))
            {
                continue;
            }

            Vec3d crystalVec = new Vec3d(pos.getX() + 0.5,
                                         pos.getY() + 1.0,
                                         pos.getZ() + 0.5);

            if (!canSeeOrWallRange(crystalVec, placeWallsRange.getValue()))
            {
                continue;
            }

            CrystalResult cached = damageCache.get(pos);
            CrystalResult result;

            if (cached != null
                && System.currentTimeMillis() - cached.time < 250L
                && cached.target == target)
            {
                result = cached;
            }
            else
            {
                float targetDamage = DamageUtil.calculate(pos, target);
                float selfDamage = DamageUtil.calculate(pos, mc.player);
                boolean antiFeet = antiFeetPlace.getValue() && isAntiFeetPlace(pos, target);

                if (!isDamageValid(target, targetDamage, selfDamage, antiFeet, pos))
                {
                    continue;
                }

                result = new CrystalResult(pos,
                                           target,
                                           targetDamage,
                                           selfDamage,
                                           System.currentTimeMillis());
                damageCache.put(pos, result);
            }

            results.add(result);
        }

        results.sort((a, b) ->
        {
            if (focusRotation.getValue())
            {
                float diffA = getRotationDiff(a.pos.getX() + 0.5,
                                              a.pos.getY() + 1.0,
                                              a.pos.getZ() + 0.5);
                float diffB = getRotationDiff(b.pos.getX() + 0.5,
                                              b.pos.getY() + 1.0,
                                              b.pos.getZ() + 0.5);

                if (Math.abs(a.targetDamage - b.targetDamage) <= 1.0f)
                {
                    return Float.compare(diffA, diffB);
                }
            }

            return Float.compare(b.targetDamage, a.targetDamage);
        });

        return results.size() > amount
            ? new ArrayList<>(results.subList(0, amount))
            : results;
    }

    private boolean isDamageValid(EntityPlayer target,
                                  float targetDamage,
                                  float selfDamage,
                                  boolean antiFeet,
                                  BlockPos pos)
    {
        float required = getRequiredDamage(target, antiFeet);

        if (targetDamage < required)
        {
            float health = target.getHealth() + target.getAbsorptionAmount();

            if (lethalCrystals.getValue() <= 0
                || targetDamage * lethalCrystals.getValue() < health)
            {
                return false;
            }
        }

        if (selfDamage > maxSelfDamage.getValue())
        {
            return false;
        }

        if (antiSuicide.getValue()
            && selfDamage >= mc.player.getHealth() + mc.player.getAbsorptionAmount() - 1.0f)
        {
            return false;
        }

        return !antiFriendPop.getValue() || !hurtsFriend(pos);
    }

    private float getRequiredDamage(EntityPlayer target, boolean antiFeet)
    {
        if (antiFeet)
        {
            return Math.min(minDamage.getValue(), antiFeetDamage.getValue());
        }

        if (facePlace.getValue()
            && target.getHealth() + target.getAbsorptionAmount() <= facePlaceHealth.getValue())
        {
            return Math.min(minDamage.getValue(), facePlaceDamage.getValue());
        }

        if (armorPlace.getValue() && getLowestArmor(target) <= armorPercent.getValue())
        {
            return Math.min(minDamage.getValue(), facePlaceDamage.getValue());
        }

        return minDamage.getValue();
    }

    private boolean hurtsFriend(BlockPos pos)
    {
        for (EntityPlayer friend : mc.world.playerEntities)
        {
            if (friend == null
                || friend == mc.player
                || EntityUtil.isDead(friend)
                || !Managers.FRIENDS.contains(friend))
            {
                continue;
            }

            float damage = DamageUtil.calculate(pos, friend);

            if (damage > maxFriendDamage.getValue())
            {
                return true;
            }
        }

        return false;
    }

    private boolean canPlaceCrystal(BlockPos base)
    {
        Block block = mc.world.getBlockState(base).getBlock();

        if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK)
        {
            return false;
        }

        BlockPos crystal = base.up();
        BlockPos crystal2 = base.up(2);

        if (!mc.world.isAirBlock(crystal))
        {
            return false;
        }

        if (onePointTwelve.getValue() && !mc.world.isAirBlock(crystal2))
        {
            return false;
        }

        for (Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(crystal)))
        {
            if (!EntityUtil.isDead(entity))
            {
                return false;
            }
        }

        if (onePointTwelve.getValue())
        {
            for (Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(crystal2)))
            {
                if (!EntityUtil.isDead(entity))
                {
                    return false;
                }
            }
        }

        return getPlaceData(base) != null;
    }

    private PlaceData getPlaceData(BlockPos base)
    {
        PlaceData strict = getPlaceData(base, strictDirection.getValue());

        if (strict != null)
        {
            return strict;
        }

        if (strictDirection.getValue() && strictFallback.getValue())
        {
            return getPlaceData(base, false);
        }

        return null;
    }

    private PlaceData getPlaceData(BlockPos base, boolean strict)
    {
        EnumFacing[] order =
        {
            EnumFacing.UP,
            EnumFacing.NORTH,
            EnumFacing.SOUTH,
            EnumFacing.EAST,
            EnumFacing.WEST
        };

        for (EnumFacing face : order)
        {
            Vec3d hitVec = new Vec3d(base)
                .add(0.5, 0.5, 0.5)
                .add(new Vec3d(face.getDirectionVec()).scale(0.5));

            if (strict && !canStrictPlace(base, face, hitVec))
            {
                continue;
            }

            return new PlaceData(
                base,
                face,
                (float) (hitVec.x - base.getX()),
                (float) (hitVec.y - base.getY()),
                (float) (hitVec.z - base.getZ())
            );
        }

        return null;
    }

    private boolean canStrictPlace(BlockPos base, EnumFacing face, Vec3d hitVec)
    {
        Vec3d eyes = mc.player.getPositionEyes(1.0f);

        if (eyes.squareDistanceTo(hitVec) > square(placeRange.getValue()))
        {
            return false;
        }

        RayTraceResult result = mc.world.rayTraceBlocks(eyes, hitVec, false, true, false);

        if (result != null
            && result.typeOfHit == RayTraceResult.Type.BLOCK
            && result.getBlockPos().equals(base)
            && result.sideHit == face)
        {
            return true;
        }

        return smartTrace.getValue() || !strictDirection.getValue();
    }

    private boolean isAntiFeetPlace(BlockPos crystalBase, EntityPlayer target)
    {
        BlockPos feet = new BlockPos(
            MathHelper.floor(target.posX),
            MathHelper.floor(target.posY),
            MathHelper.floor(target.posZ)
        );

        return crystalBase.up().distanceSq(feet) <= 4.0;
    }

    private void onCrystalSpawn(SPacketSpawnObject packet)
    {
        if (!predict.getValue()
            || !breakCrystals.getValue()
            || mc.player == null
            || mc.world == null
            || packet.getType() != 51)
        {
            return;
        }

        EntityPlayer currentTarget = target == null ? findTarget() : target;

        if (currentTarget == null)
        {
            return;
        }

        Vec3d pos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());

        if (mc.player.getDistanceSq(pos.x, pos.y, pos.z) > square(breakRange.getValue()))
        {
            return;
        }

        float targetDamage = DamageUtil.calculate(packet.getX(), packet.getY(), packet.getZ(), currentTarget);
        float selfDamage = DamageUtil.calculate(packet.getX(), packet.getY(), packet.getZ(), mc.player);

        if (!isDamageValid(currentTarget,
                           targetDamage,
                           selfDamage,
                           false,
                           new BlockPos(packet.getX(), packet.getY() - 1.0, packet.getZ())))
        {
            return;
        }

        if (rotate.getValue() && rotationMode.getValue() == RotationMode.PACKET)
        {
            float[] r = calculateRotations(packet.getX(), packet.getY(), packet.getZ());
            PacketUtil.doRotation(r[0], r[1], mc.player.onGround);
        }

        PacketUtil.attack(packet.getEntityID());
        swing(EnumHand.MAIN_HAND);
        attackedCrystals.put(packet.getEntityID(), System.currentTimeMillis());
        CombatCoordinator.markCrystalAttacked(packet.getEntityID());
        cps.add(System.currentTimeMillis());
        lastBreak = System.currentTimeMillis();

        if (sequence.getValue() == Sequence.STRONG && placeResult != null)
        {
            doPlace();
        }
    }

    private void onDestroyEntities(SPacketDestroyEntities packet)
    {
        for (int id : packet.getEntityIDs())
        {
            attackedCrystals.remove(id);
        }
    }

    private boolean canSeeOrWallRange(Vec3d vec, float wallsRange)
    {
        Vec3d eyes = mc.player.getPositionEyes(1.0f);
        RayTraceResult result = mc.world.rayTraceBlocks(eyes, vec, false, true, false);

        if (result == null || result.typeOfHit == RayTraceResult.Type.MISS)
        {
            return true;
        }

        if (eyes.squareDistanceTo(vec) <= square(wallsRange))
        {
            return true;
        }

        return rayBypass.getValue() && smartTrace.getValue();
    }

    private boolean isMultiTaskBlocked()
    {
        if (!mc.player.isHandActive())
        {
            return false;
        }

        switch (multiTask.getValue())
        {
            case NONE:
            case STRONG:
                return true;
            case SOFT:
                return mc.player.getActiveHand() != EnumHand.OFF_HAND;
            default:
                return false;
        }
    }

    private boolean isMining()
    {
        return mc.gameSettings.keyBindAttack.isKeyDown()
            && mc.objectMouseOver != null
            && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK;
    }

    private int findWeaponSlot()
    {
        int pickaxe = -1;

        for (int i = 0; i < 9; i++)
        {
            if (mc.player.inventory.getStackInSlot(i).getItem() instanceof ItemSword)
            {
                return i;
            }

            if (pickaxe == -1
                && mc.player.inventory.getStackInSlot(i).getItem() instanceof ItemPickaxe)
            {
                pickaxe = i;
            }
        }

        return pickaxe;
    }

    private void switchTo(int slot)
    {
        if (slot < 0 || slot > 8 || mc.player.inventory.currentItem == slot)
        {
            return;
        }

        mc.player.connection.sendPacket(new CPacketHeldItemChange(slot));
        mc.player.inventory.currentItem = slot;
        InventoryUtil.syncItem();
        lastSwitch = System.currentTimeMillis();
    }

    private void swing(EnumHand hand)
    {
        if (handSwing.getValue() == HandSwing.NONE)
        {
            return;
        }

        if (handSwing.getValue() == HandSwing.CLIENT
            || handSwing.getValue() == HandSwing.BOTH)
        {
            mc.player.swingArm(hand);
        }

        if (handSwing.getValue() == HandSwing.PACKET
            || handSwing.getValue() == HandSwing.BOTH)
        {
            mc.player.connection.sendPacket(new CPacketAnimation(hand));
        }
    }

    private EnumHand getSwingHand(SwingHand setting, EnumHand fallback)
    {
        switch (setting)
        {
            case MAINHAND:
                return EnumHand.MAIN_HAND;
            case OFFHAND:
                return EnumHand.OFF_HAND;
            case AUTO:
            default:
                return fallback;
        }
    }

    private List<BlockPos> getSphere(BlockPos center, int radius)
    {
        List<BlockPos> positions = new ArrayList<>();

        for (int x = center.getX() - radius; x <= center.getX() + radius; x++)
        {
            for (int y = center.getY() - radius; y <= center.getY() + radius; y++)
            {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++)
                {
                    BlockPos pos = new BlockPos(x, y, z);

                    if (center.distanceSq(pos) <= radius * radius)
                    {
                        positions.add(pos);
                    }
                }
            }
        }

        positions.sort(Comparator.comparingDouble(p -> mc.player.getDistanceSq(p)));
        return positions;
    }

    private BlockPos getPlayerPos()
    {
        return new BlockPos(MathHelper.floor(mc.player.posX),
                            MathHelper.floor(mc.player.posY),
                            MathHelper.floor(mc.player.posZ));
    }

    private float[] calculateRotations(double x, double y, double z)
    {
        double diffX = x - mc.player.posX;
        double diffY = y - (mc.player.posY + mc.player.getEyeHeight());
        double diffZ = z - mc.player.posZ;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        return new float[]
        {
            (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f,
            (float) -Math.toDegrees(Math.atan2(diffY, dist))
        };
    }

    private float[] smooth(float yaw, float pitch)
    {
        float yawDiff = MathHelper.wrapDegrees(yaw - lastYaw);
        float pitchDiff = pitch - lastPitch;

        lastYaw += yawDiff * smoothSpeed.getValue();
        lastPitch += pitchDiff * smoothSpeed.getValue();

        return new float[] { lastYaw, lastPitch };
    }

    private float getRotationDiff(double x, double y, double z)
    {
        float[] r = calculateRotations(x, y, z);
        return Math.abs(MathHelper.wrapDegrees(r[0] - mc.player.rotationYaw))
            + Math.abs(r[1] - mc.player.rotationPitch);
    }

    private int getCps()
    {
        long now = System.currentTimeMillis();
        cps.removeIf(time -> now - time > 1000L);
        return cps.size();
    }

    private boolean passed(long last, int delay)
    {
        return System.currentTimeMillis() - last >= delay;
    }

    private double square(double value)
    {
        return value * value;
    }

    private boolean isNaked(EntityPlayer player)
    {
        for (ItemStack stack : player.getArmorInventoryList())
        {
            if (!stack.isEmpty())
            {
                return false;
            }
        }

        return true;
    }

    private float getLowestArmor(EntityPlayer player)
    {
        float lowest = 100.0f;
        boolean found = false;

        for (ItemStack stack : player.getArmorInventoryList())
        {
            if (stack.isEmpty() || stack.getMaxDamage() <= 0)
            {
                continue;
            }

            found = true;
            float durability =
                ((stack.getMaxDamage() - stack.getItemDamage()) / (float) stack.getMaxDamage()) * 100.0f;

            if (durability < lowest)
            {
                lowest = durability;
            }
        }

        return found ? lowest : 0.0f;
    }

    private void cleanCaches()
    {
        long now = System.currentTimeMillis();

        attackedCrystals.entrySet().removeIf(e -> now - e.getValue() > 1000L);
        placedCrystals.entrySet().removeIf(e -> now - e.getValue() > 1000L);
        damageCache.entrySet().removeIf(e -> now - e.getValue().time > 250L);
        renderMap.entrySet().removeIf(e -> now - e.getValue().time > renderTime.getValue());
        cps.removeIf(time -> now - time > 1000L);

        CombatCoordinator.clean();
    }

    private void onRender(Render3DEvent event)
    {
        if (!render.getValue() || mc.player == null || mc.world == null)
        {
            return;
        }

        long now = System.currentTimeMillis();
        int fade = Math.max(1, renderTime.getValue());

        for (Map.Entry<BlockPos, RenderEntry> entry : renderMap.entrySet())
        {
            RenderEntry value = entry.getValue();
            long age = now - value.time;

            if (age > fade)
            {
                continue;
            }

            Color f = fill.getValue();
            Color l = line.getValue();

            int fa = renderMode.getValue() == RenderMode.FADE
                ? MathHelper.clamp((int) (f.getAlpha() * (1.0f - age / (float) fade)), 0, f.getAlpha())
                : f.getAlpha();

            int la = renderMode.getValue() == RenderMode.FADE
                ? MathHelper.clamp((int) (l.getAlpha() * (1.0f - age / (float) fade)), 0, l.getAlpha())
                : l.getAlpha();

            RenderUtil.renderBox(
                Interpolation.interpolatePos(value.pos, 1.0f),
                new Color(f.getRed(), f.getGreen(), f.getBlue(), fa),
                new Color(l.getRed(), l.getGreen(), l.getBlue(), la),
                1.5f
            );

            // DamageTextは環境差が大きいので、ここではBox/Fadeまで。
            // 3D文字描画はFontRenderer+GLStateManagerで別途追加するのが安全。
        }
    }

    private static final class CrystalResult
    {
        final BlockPos pos;
        final EntityPlayer target;
        final float targetDamage;
        final float selfDamage;
        final long time;

        CrystalResult(BlockPos pos,
                      EntityPlayer target,
                      float targetDamage,
                      float selfDamage,
                      long time)
        {
            this.pos = pos;
            this.target = target;
            this.targetDamage = targetDamage;
            this.selfDamage = selfDamage;
            this.time = time;
        }
    }

    private static final class PlaceData
    {
        final BlockPos base;
        final EnumFacing face;
        final float hitX;
        final float hitY;
        final float hitZ;

        PlaceData(BlockPos base, EnumFacing face, float hitX, float hitY, float hitZ)
        {
            this.base = base;
            this.face = face;
            this.hitX = hitX;
            this.hitY = hitY;
            this.hitZ = hitZ;
        }
    }

    private static final class RenderEntry
    {
        final BlockPos pos;
        final float damage;
        final long time;

        RenderEntry(BlockPos pos, float damage, long time)
        {
            this.pos = pos;
            this.damage = damage;
            this.time = time;
        }
    }

    public enum TargetMode
    {
        DAMAGE,
        RANGE,
        HEALTH
    }

    public enum AutoSwitch
    {
        NONE,
        NORMAL,
        SILENT
    }

    public enum AntiWeakness
    {
        NONE,
        NORMAL,
        SILENT
    }

    public enum MultiTask
    {
        NONE,
        SOFT,
        STRONG
    }

    public enum RotationMode
    {
        MOTION,
        PACKET
    }

    public enum Sequence
    {
        SOFT,
        STRONG
    }

    public enum Priority
    {
        CRYSTAL,
        BALANCED,
        FEET_TRAP
    }

    public enum HandSwing
    {
        NONE,
        CLIENT,
        PACKET,
        BOTH
    }

    public enum SwingHand
    {
        AUTO,
        MAINHAND,
        OFFHAND
    }

    public enum RenderMode
    {
        NORMAL,
        FADE
    }

    private static final class ListenerMotion
        extends ModuleListener<AutoCrystal, MotionUpdateEvent>
    {
        private ListenerMotion(AutoCrystal module)
        {
            super(module, MotionUpdateEvent.class, -999999999);
        }

        @Override
        public void invoke(MotionUpdateEvent event)
        {
            module.onMotion(event);
        }
    }

    private static final class ListenerSpawnObject
        extends ModuleListener<AutoCrystal, PacketEvent.Receive<SPacketSpawnObject>>
    {
        private ListenerSpawnObject(AutoCrystal module)
        {
            super(module, PacketEvent.Receive.class, SPacketSpawnObject.class);
        }

        @Override
        public void invoke(PacketEvent.Receive<SPacketSpawnObject> event)
        {
            module.onCrystalSpawn(event.getPacket());
        }
    }

    private static final class ListenerDestroyEntities
        extends ModuleListener<AutoCrystal, PacketEvent.Receive<SPacketDestroyEntities>>
    {
        private ListenerDestroyEntities(AutoCrystal module)
        {
            super(module, PacketEvent.Receive.class, SPacketDestroyEntities.class);
        }

        @Override
        public void invoke(PacketEvent.Receive<SPacketDestroyEntities> event)
        {
            module.onDestroyEntities(event.getPacket());
        }
    }

    private static final class ListenerRender
        extends ModuleListener<AutoCrystal, Render3DEvent>
    {
        private ListenerRender(AutoCrystal module)
        {
            super(module, Render3DEvent.class);
        }

        @Override
        public void invoke(Render3DEvent event)
        {
            module.onRender(event);
        }
    }
}

