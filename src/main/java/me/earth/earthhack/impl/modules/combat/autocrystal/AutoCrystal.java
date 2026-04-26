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
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.minecraft.DamageUtil;
import me.earth.earthhack.impl.util.minecraft.InventoryUtil;
import me.earth.earthhack.impl.util.minecraft.entity.EntityUtil;
import me.earth.earthhack.impl.util.network.PacketUtil;
import me.earth.earthhack.impl.util.render.Interpolation;
import me.earth.earthhack.impl.util.render.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.CPacketHeldItemChange;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AutoCrystal extends Module
{
    protected final Setting<TargetMode> targetMode =
        register(new EnumSetting<>("Target", TargetMode.DAMAGE));

    protected final Setting<Float> targetRange =
        register(new NumberSetting<>("TargetRange", 10.0f, 1.0f, 20.0f));

    protected final Setting<Boolean> place =
        register(new BooleanSetting("Place", true));

    protected final Setting<Float> placeRange =
        register(new NumberSetting<>("PlaceRange", 5.5f, 1.0f, 6.0f));

    protected final Setting<Float> placeWallsRange =
        register(new NumberSetting<>("PlaceWalls", 3.0f, 0.0f, 6.0f));

    protected final Setting<Integer> placeDelay =
        register(new NumberSetting<>("PlaceDelay", 50, 0, 1000));

    protected final Setting<Float> minDamage =
        register(new NumberSetting<>("MinDamage", 4.0f, 0.0f, 36.0f));

    protected final Setting<Float> maxSelfDamage =
        register(new NumberSetting<>("MaxSelfDmg", 8.0f, 0.0f, 36.0f));

    protected final Setting<Boolean> antiSuicide =
        register(new BooleanSetting("AntiSuicide", true));

    protected final Setting<Integer> lethalCrystals =
        register(new NumberSetting<>("LethalCrystals", 0, 0, 5));

    protected final Setting<Boolean> onePointTwelve =
        register(new BooleanSetting("1.12", true));

    protected final Setting<Boolean> antiFeetPlace =
        register(new BooleanSetting("AntiFeetPlace", false));

    protected final Setting<Float> antiFeetMinDamage =
        register(new NumberSetting<>("AFP-MinDmg", 2.0f, 0.0f, 10.0f));

    protected final Setting<Boolean> breakCrystals =
        register(new BooleanSetting("Break", true));

    protected final Setting<Float> breakRange =
        register(new NumberSetting<>("BreakRange", 5.8f, 1.0f, 6.0f));

    protected final Setting<Float> breakWallsRange =
        register(new NumberSetting<>("BreakWalls", 3.0f, 0.0f, 6.0f));

    protected final Setting<Integer> breakDelay =
        register(new NumberSetting<>("BreakDelay", 50, 0, 1000));

    protected final Setting<Integer> ticksExisted =
        register(new NumberSetting<>("TicksExisted", 0, 0, 20));

    protected final Setting<Boolean> predict =
        register(new BooleanSetting("Predict", true));

    protected final Setting<Boolean> autoHit =
        register(new BooleanSetting("AutoHit", false));

    protected final Setting<Boolean> inhibit =
        register(new BooleanSetting("Inhibit", true));

    protected final Setting<Boolean> setDead =
        register(new BooleanSetting("SetDead", false));

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

    protected final Setting<Boolean> rotate =
        register(new BooleanSetting("Rotate", true));

    protected final Setting<RotationMode> rotationMode =
        register(new EnumSetting<>("RotationMode", RotationMode.MOTION));

    protected final Setting<Sequence> sequence =
        register(new EnumSetting<>("Sequence", Sequence.SOFT));

    protected final Setting<SwapWait> swapWait =
        register(new EnumSetting<>("SwapWait", SwapWait.NONE));

    protected final Setting<Integer> swapWaitDelay =
        register(new NumberSetting<>("SwapWaitDelay", 150, 0, 1000));

    protected final Setting<Integer> cpsLimit =
        register(new NumberSetting<>("CPSLimit", 0, 0, 20));

    protected final Setting<Boolean> render =
        register(new BooleanSetting("Render", true));

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
    private final Map<BlockPos, RenderEntry> renderMap = new ConcurrentHashMap<>();
    private final LinkedList<Long> cps = new LinkedList<>();

    private EntityPlayer target;
    private EntityEnderCrystal breakCrystal;
    private CrystalResult placeResult;
    private BlockPos renderPos;
    private float renderDamage;
    private float[] rotations;

    private long lastPlace;
    private long lastBreak;
    private long lastSwitch;

    public AutoCrystal()
    {
        super("AutoCrystal", Category.Combat);

        this.listeners.add(new ListenerMotion(this));
        this.listeners.add(new ListenerSpawnObject(this));
        this.listeners.add(new ListenerDestroyEntities(this));
        this.listeners.add(new ListenerRender(this));

        this.setData(new SimpleData(this,
            "Standalone 1.12.2 AutoCrystal inspired by Sn0w CatAura.",
            0xffffffff));
    }

    @Override
    protected void onEnable()
    {
        attackedCrystals.clear();
        placedCrystals.clear();
        renderMap.clear();
        cps.clear();

        target = null;
        breakCrystal = null;
        placeResult = null;
        renderPos = null;
        rotations = null;
        renderDamage = 0.0f;

        lastPlace = 0L;
        lastBreak = 0L;
        lastSwitch = 0L;
    }

    @Override
    protected void onDisable()
    {
        attackedCrystals.clear();
        placedCrystals.clear();
        renderMap.clear();
        cps.clear();

        target = null;
        breakCrystal = null;
        placeResult = null;
        renderPos = null;
        rotations = null;
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

        if (target == null)
        {
            renderPos = null;
            return;
        }

        if (isMultiTaskBlocked())
        {
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
            rotations = getRotations(
                breakCrystal.posX,
                breakCrystal.posY + breakCrystal.height * 0.5,
                breakCrystal.posZ
            );
        }
        else if (placeResult != null)
        {
            rotations = getRotations(
                placeResult.pos.getX() + 0.5,
                placeResult.pos.getY() + 1.0,
                placeResult.pos.getZ() + 0.5
            );
        }

        if (placeResult != null)
        {
            renderPos = placeResult.pos;
            renderDamage = placeResult.targetDamage;
        }
    }

    private void doBreak()
    {
        if (breakCrystal == null)
        {
            return;
        }

        if (!passed(lastBreak, breakDelay.getValue()))
        {
            return;
        }

        if (cpsLimit.getValue() > 0 && getCps() >= cpsLimit.getValue())
        {
            return;
        }

        if (inhibit.getValue() && attackedCrystals.containsKey(breakCrystal.getEntityId()))
        {
            return;
        }

        if (mc.player.getDistanceSq(breakCrystal) > square(breakRange.getValue()))
        {
            return;
        }

        if (swapWait.getValue() != SwapWait.NONE
            && System.currentTimeMillis() - lastSwitch < swapWaitDelay.getValue())
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

        attackCrystal(breakCrystal);

        if (switched && antiWeakness.getValue() == AntiWeakness.SILENT)
        {
            switchTo(oldSlot);
        }

        if (setDead.getValue())
        {
            mc.world.removeEntityFromWorld(breakCrystal.getEntityId());
        }

        attackedCrystals.put(breakCrystal.getEntityId(), System.currentTimeMillis());
        cps.add(System.currentTimeMillis());
        lastBreak = System.currentTimeMillis();
    }

    private void attackCrystal(EntityEnderCrystal crystal)
    {
        if (rotate.getValue() && rotationMode.getValue() == RotationMode.PACKET)
        {
            float[] r = getRotations(crystal.posX,
                                     crystal.posY + crystal.height * 0.5,
                                     crystal.posZ);
            PacketUtil.doRotation(r[0], r[1], mc.player.onGround);
        }

        PacketUtil.attack(crystal);
    }

    private void doPlace()
    {
        if (placeResult == null)
        {
            return;
        }

        if (!passed(lastPlace, placeDelay.getValue()))
        {
            return;
        }

        if (swapWait.getValue() == SwapWait.FULL
            && System.currentTimeMillis() - lastSwitch < swapWaitDelay.getValue())
        {
            return;
        }

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

        EnumHand hand = offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;

        if (rotate.getValue() && rotationMode.getValue() == RotationMode.PACKET)
        {
            float[] r = getRotations(placeResult.pos.getX() + 0.5,
                                     placeResult.pos.getY() + 1.0,
                                     placeResult.pos.getZ() + 0.5);
            PacketUtil.doRotation(r[0], r[1], mc.player.onGround);
        }

        PlaceData data = getPlaceData(placeResult.pos);

        if (data == null)
        {
            if (switched && autoSwitch.getValue() == AutoSwitch.SILENT)
            {
                switchTo(oldSlot);
            }

            return;
        }

        PacketUtil.place(data.base,
                         data.face,
                         hand,
                         data.hitX,
                         data.hitY,
                         data.hitZ);

        PacketUtil.swing(offhand ? -2 : mc.player.inventory.currentItem);

        long now = System.currentTimeMillis();
        placedCrystals.put(placeResult.pos, now);
        renderMap.put(placeResult.pos,
            new RenderEntry(placeResult.pos, placeResult.targetDamage, now));
        renderPos = placeResult.pos;
        renderDamage = placeResult.targetDamage;
        lastPlace = now;

        if (switched && autoSwitch.getValue() == AutoSwitch.SILENT)
        {
            switchTo(oldSlot);
        }
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

            if (targetMode.getValue() == TargetMode.RANGE)
            {
                double dist = mc.player.getDistanceSq(player);

                if (dist < bestValue)
                {
                    bestValue = dist;
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

            if (!canSeeOrWallRange(crystal.getPositionVector(), breakWallsRange.getValue()))
            {
                continue;
            }

            float targetDamage = DamageUtil.calculate(crystal, target);
            float selfDamage = DamageUtil.calculate(crystal, mc.player);

            if (!isDamageValid(target, targetDamage, selfDamage, false))
            {
                continue;
            }

            if (targetDamage > bestDamage)
            {
                bestDamage = targetDamage;
                best = crystal;
            }
        }

        return best;
    }

    private CrystalResult findBestPlace(EntityPlayer target)
    {
        List<BlockPos> sphere = getSphere(getPlayerPos(), MathHelper.ceil(placeRange.getValue()));
        CrystalResult best = null;

        for (BlockPos pos : sphere)
        {
            if (!canPlaceCrystal(pos))
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

            float targetDamage = DamageUtil.calculate(pos, target);
            float selfDamage = DamageUtil.calculate(pos, mc.player);
            boolean antiFeet = antiFeetPlace.getValue() && isAntiFeetPlace(pos, target);

            if (!isDamageValid(target, targetDamage, selfDamage, antiFeet))
            {
                continue;
            }

            if (best == null || targetDamage > best.targetDamage)
            {
                best = new CrystalResult(pos, targetDamage, selfDamage);
            }
        }

        return best;
    }

    private boolean isDamageValid(EntityPlayer target,
                                  float targetDamage,
                                  float selfDamage,
                                  boolean antiFeet)
    {
        float required = antiFeet
            ? Math.min(minDamage.getValue(), antiFeetMinDamage.getValue())
            : minDamage.getValue();

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

        return !antiSuicide.getValue()
            || selfDamage < mc.player.getHealth() + mc.player.getAbsorptionAmount() - 1.0f;
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

        AxisAlignedBB bb1 = new AxisAlignedBB(crystal);

        for (Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, bb1))
        {
            if (!EntityUtil.isDead(entity))
            {
                return false;
            }
        }

        if (onePointTwelve.getValue())
        {
            AxisAlignedBB bb2 = new AxisAlignedBB(crystal2);

            for (Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, bb2))
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

            float x = (float) (hitVec.x - base.getX());
            float y = (float) (hitVec.y - base.getY());
            float z = (float) (hitVec.z - base.getZ());

            return new PlaceData(base, face, x, y, z);
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

        RayTraceResult result = mc.world.rayTraceBlocks(
            eyes,
            hitVec,
            false,
            true,
            false
        );

        if (result != null
            && result.typeOfHit == RayTraceResult.Type.BLOCK
            && result.getBlockPos().equals(base)
            && result.sideHit == face)
        {
            return true;
        }

        return isOnVisibleSide(eyes, base, face);
    }

    private boolean isOnVisibleSide(Vec3d eyes, BlockPos pos, EnumFacing face)
    {
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;

        switch (face)
        {
            case DOWN:
                return eyes.y < centerY;
            case UP:
                return eyes.y > centerY;
            case NORTH:
                return eyes.z < centerZ;
            case SOUTH:
                return eyes.z > centerZ;
            case WEST:
                return eyes.x < centerX;
            case EAST:
                return eyes.x > centerX;
            default:
                return false;
        }
    }

    private boolean isAntiFeetPlace(BlockPos crystalBase, EntityPlayer target)
    {
        BlockPos feet = new BlockPos(
            MathHelper.floor(target.posX),
            MathHelper.floor(target.posY),
            MathHelper.floor(target.posZ)
        );

        BlockPos crystal = crystalBase.up();

        return crystal.distanceSq(feet) <= 4.0;
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

        if (!autoHit.getValue()
            && !canSeeOrWallRange(pos, breakWallsRange.getValue()))
        {
            return;
        }

        float targetDamage = DamageUtil.calculate(packet.getX(),
                                                  packet.getY(),
                                                  packet.getZ(),
                                                  currentTarget);

        float selfDamage = DamageUtil.calculate(packet.getX(),
                                                packet.getY(),
                                                packet.getZ(),
                                                mc.player);

        if (!isDamageValid(currentTarget, targetDamage, selfDamage, false))
        {
            return;
        }

        if (rotate.getValue())
        {
            float[] r = getRotations(packet.getX(), packet.getY(), packet.getZ());

            if (rotationMode.getValue() == RotationMode.PACKET)
            {
                PacketUtil.doRotation(r[0], r[1], mc.player.onGround);
            }
        }

        PacketUtil.attack(packet.getEntityID());
        attackedCrystals.put(packet.getEntityID(), System.currentTimeMillis());
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
            if (attackedCrystals.remove(id) != null)
            {
                cps.add(System.currentTimeMillis());
            }
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

        return eyes.squareDistanceTo(vec) <= square(wallsRange);
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
                return true;
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

    private List<BlockPos> getSphere(BlockPos center, int radius)
    {
        List<BlockPos> positions = new ArrayList<>();

        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();

        for (int x = cx - radius; x <= cx + radius; x++)
        {
            for (int y = cy - radius; y <= cy + radius; y++)
            {
                for (int z = cz - radius; z <= cz + radius; z++)
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

    private float[] getRotations(double x, double y, double z)
    {
        double diffX = x - mc.player.posX;
        double diffY = y - (mc.player.posY + mc.player.getEyeHeight());
        double diffZ = z - mc.player.posZ;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));

        return new float[]
        {
            yaw,
            pitch
        };
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

    private void cleanCaches()
    {
        long now = System.currentTimeMillis();

        attackedCrystals.entrySet().removeIf(e -> now - e.getValue() > 1000L);
        placedCrystals.entrySet().removeIf(e -> now - e.getValue() > 1000L);
        renderMap.entrySet().removeIf(e -> now - e.getValue().time > renderTime.getValue());
        cps.removeIf(time -> now - time > 1000L);
    }

    private void onRender(Render3DEvent event)
    {
        if (!render.getValue()
            || mc.player == null
            || mc.world == null)
        {
            return;
        }

        if (renderMode.getValue() == RenderMode.NORMAL)
        {
            if (renderPos == null)
            {
                return;
            }

            RenderUtil.renderBox(
                Interpolation.interpolatePos(renderPos, 1.0f),
                fill.getValue(),
                line.getValue(),
                1.5f
            );

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
                ? MathHelper.clamp((int) (f.getAlpha() * (1.0f - age / (float) fade)),
                                   0,
                                   f.getAlpha())
                : f.getAlpha();

            int la = renderMode.getValue() == RenderMode.FADE
                ? MathHelper.clamp((int) (l.getAlpha() * (1.0f - age / (float) fade)),
                                   0,
                                   l.getAlpha())
                : l.getAlpha();

            RenderUtil.renderBox(
                Interpolation.interpolatePos(value.pos, 1.0f),
                new Color(f.getRed(), f.getGreen(), f.getBlue(), fa),
                new Color(l.getRed(), l.getGreen(), l.getBlue(), la),
                1.5f
            );
        }
    }

    private static final class CrystalResult
    {
        final BlockPos pos;
        final float targetDamage;
        final float selfDamage;

        CrystalResult(BlockPos pos, float targetDamage, float selfDamage)
        {
            this.pos = pos;
            this.targetDamage = targetDamage;
            this.selfDamage = selfDamage;
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
        RANGE
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
        NONE,
        SOFT,
        STRONG
    }

    public enum SwapWait
    {
        NONE,
        SEMI,
        FULL
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