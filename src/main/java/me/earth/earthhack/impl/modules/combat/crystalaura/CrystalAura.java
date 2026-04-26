package me.earth.earthhack.impl.modules.combat.crystalaura;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BindSetting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.api.util.bind.Bind;
import me.earth.earthhack.impl.core.ducks.entity.IEntity;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.combat.autocrystal.CrystalPositionCache;
import me.earth.earthhack.impl.modules.combat.autocrystal.SwapStateTracker;
import me.earth.earthhack.impl.util.math.GuardTimer;
import me.earth.earthhack.impl.util.minecraft.DamageUtil;
import me.earth.earthhack.impl.util.minecraft.InventoryUtil;
import me.earth.earthhack.impl.util.network.PacketUtil;
import me.earth.earthhack.impl.util.thread.Locks;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

public class CrystalAura extends Module
{
    protected final Setting<Float> placeRange = register(new NumberSetting<>("PlaceRange", 4.5f, 0.0f, 8.0f));
    protected final Setting<Float> breakRange = register(new NumberSetting<>("BreakRange", 4.8f, 0.0f, 8.0f));
    protected final Setting<Float> wallRange = register(new NumberSetting<>("WallRange", 3.0f, 0.0f, 8.0f));
    protected final Setting<Integer> placeDelay = register(new NumberSetting<>("PlaceDelay", 45, 0, 500));
    protected final Setting<Integer> breakDelay = register(new NumberSetting<>("BreakDelay", 45, 0, 500));
    protected final Setting<Float> minDamage = register(new NumberSetting<>("MinDamage", 6.0f, 0.0f, 20.0f));
    protected final Setting<Float> maxSelfDamage = register(new NumberSetting<>("MaxSelfDamage", 8.0f, 0.0f, 20.0f));

    protected final Setting<Boolean> rotate = register(new BooleanSetting("Rotate", true));
    protected final Setting<Boolean> predictSpawn = register(new BooleanSetting("Predict", true));
    protected final Setting<Boolean> antiWeakness = register(new BooleanSetting("AntiWeakness", true));
    protected final Setting<Boolean> antiSuicide = register(new BooleanSetting("AntiSuicide", true));
    protected final Setting<Boolean> raytrace = register(new BooleanSetting("Raytrace", true));
    protected final Setting<Boolean> facePlace = register(new BooleanSetting("FacePlace", true));
    protected final Setting<Float> facePlaceHealth = register(new NumberSetting<>("FacePlaceHealth", 10.0f, 0.0f, 36.0f));
    protected final Setting<Integer> armorBreaker = register(new NumberSetting<>("ArmorBreaker", 12, 0, 100));
    protected final Setting<Boolean> multiThread = register(new BooleanSetting("MultiThread", false));

    protected final Setting<Boolean> setDead = register(new BooleanSetting("SetDead", false));
    protected final Setting<Boolean> pseudoDead = register(new BooleanSetting("PseudoDead", false));
    protected final Setting<Boolean> soundRemove = register(new BooleanSetting("SoundRemove", true));
    protected final Setting<Integer> deathTime = register(new NumberSetting<>("DeathTime", 0, 0, 500));
    protected final Setting<Boolean> useSafeDeathTime = register(new BooleanSetting("UseSafeDeathTime", false));
    protected final Setting<Integer> safeDeathTime = register(new NumberSetting<>("SafeDeathTime", 0, 0, 500));

    protected final Setting<Boolean> sequential = register(new BooleanSetting("Sequential", true));
    protected final Setting<Integer> seqTime = register(new NumberSetting<>("SeqTime", 250, 0, 1000));
    protected final Setting<Boolean> endSequenceOnSpawn = register(new BooleanSetting("EndSeqOnSpawn", true));
    protected final Setting<Boolean> endSequenceOnBreak = register(new BooleanSetting("EndSeqOnBreak", true));
    protected final Setting<Boolean> endSequenceOnExplosion = register(new BooleanSetting("EndSeqOnExplosion", true));
    protected final Setting<Boolean> antiPlaceFail = register(new BooleanSetting("AntiPlaceFail", true));

    protected final Setting<AutoSwitchMode> autoSwitch = register(new EnumSetting<>("AutoSwitch", AutoSwitchMode.Always));
    protected final Setting<Bind> switchBind = register(new BindSetting("SwitchBind", Bind.none()));
    protected final Setting<Boolean> switchBack = register(new BooleanSetting("SwitchBack", true));
    protected final Setting<Integer> swapDelay = register(new NumberSetting<>("SwapDelay", 0, 0, 500));

    protected final Setting<TargetMode> targetMode = register(new EnumSetting<>("Target", TargetMode.Distance));

    protected final GuardTimer placeTimer = new GuardTimer();
    protected final GuardTimer breakTimer = new GuardTimer();
    protected final GuardTimer seqTimer = new GuardTimer();

    protected final CrystalPositionCache positionCache = new CrystalPositionCache();
    protected final SwapStateTracker swapStateTracker = new SwapStateTracker();
    protected final Object stateLock = new Object();
    protected final AtomicBoolean calculating = new AtomicBoolean(false);

    protected EntityPlayer target;
    protected BlockPos expecting;
    protected boolean switching;

    public CrystalAura()
    {
        super("CrystalAura", Category.Combat);
        this.listeners.add(new ListenerTick(this));
        this.listeners.add(new ListenerKeyboard(this));
        this.listeners.add(new ListenerSpawnObject(this));
        this.listeners.add(new ListenerDestroyEntities(this));
        this.listeners.add(new ListenerSound(this));
        this.setData(new CrystalAuraData(this));
    }

    @Override
    protected void onDisable()
    {
        synchronized (stateLock)
        {
            positionCache.clear();
            swapStateTracker.clear();
            clearSequence();
            switching = false;
            calculating.set(false);
        }
    }

    @Override
    public String getDisplayInfo()
    {
        return target == null ? null : target.getName();
    }

    protected void runThreadedTick()
    {
        if (multiThread.getValue())
        {
            if (!calculating.compareAndSet(false, true))
            {
                return;
            }

            Managers.THREAD.submitRunnable(() ->
            {
                try
                {
                    synchronized (stateLock)
                    {
                        runAura();
                    }
                }
                finally
                {
                    calculating.set(false);
                }
            });
        }
        else
        {
            calculating.set(true);
            synchronized (stateLock)
            {
                try
                {
                    runAura();
                }
                finally
                {
                    calculating.set(false);
                }
            }
        }
    }

    protected void runAura()
    {
        if (mc.player == null || mc.world == null)
        {
            return;
        }

        if (sequential.getValue() && expecting != null && !seqTimer.passed(seqTime.getValue()))
        {
            return;
        }

        if (!swapStateTracker.hasPassedDelay(swapDelay.getValue()))
        {
            return;
        }

        target = findTarget();
        if (target == null)
        {
            return;
        }

        if (breakTimer.passed(breakDelay.getValue()))
        {
            EntityEnderCrystal crystal = findBestCrystal(target);
            if (crystal != null)
            {
                attackCrystal(crystal);
                breakTimer.reset(breakDelay.getValue());
                return;
            }
        }

        if (placeTimer.passed(placeDelay.getValue()))
        {
            BlockPos place = findBestPlace(target);
            if (place != null)
            {
                placeCrystal(place);
                placeTimer.reset(placeDelay.getValue());
            }
            else if (antiPlaceFail.getValue() && expecting != null)
            {
                clearSequence();
                placeTimer.setTime(0L);
            }
        }
    }

    protected EntityPlayer findTarget()
    {
        Comparator<EntityPlayer> comparator = targetMode.getValue() == TargetMode.Health
            ? Comparator.comparingDouble(EntityPlayer::getHealth)
            : Comparator.comparingDouble(p -> mc.player.getDistanceSq(p));

        EntityPlayer best = null;
        for (EntityPlayer player : mc.world.playerEntities)
        {
            if (player == null
                || player == mc.player
                || player.isDead
                || player.getHealth() <= 0.0f
                || Managers.FRIENDS.contains(player)
                || mc.player.getDistanceSq(player) > sq(placeRange.getValue() + 6.0f))
            {
                continue;
            }

            if (best == null || comparator.compare(player, best) < 0)
            {
                best = player;
            }
        }

        return best;
    }

    protected BlockPos findBestPlace(EntityPlayer enemy)
    {
        BlockPos center = new BlockPos(enemy.posX, enemy.posY, enemy.posZ);
        int r = MathHelper.ceil(placeRange.getValue());
        BlockPos bestPos = null;
        float bestDamage = 0.0f;

        for (int x = -r; x <= r; x++)
        {
            for (int y = -r; y <= r; y++)
            {
                for (int z = -r; z <= r; z++)
                {
                    BlockPos pos = center.add(x, y, z);
                    BlockPos crystalPos = pos.up();
                    if (!isValidBase(pos)
                        || positionCache.isBlocked(crystalPos)
                        || mc.player.getDistanceSq(pos) > sq(placeRange.getValue())
                        || (raytrace.getValue() && !canPlaceRangeTrace(pos)))
                    {
                        continue;
                    }

                    float targetDamage = DamageUtil.calculate(pos, enemy);
                    float selfDamage = DamageUtil.calculate(pos);
                    float requiredDamage = shouldFacePlace(enemy) ? 1.0f : minDamage.getValue();
                    if (targetDamage < requiredDamage || selfDamage > maxSelfDamage.getValue())
                    {
                        continue;
                    }

                    if (antiSuicide.getValue() && selfDamage >= mc.player.getHealth() + mc.player.getAbsorptionAmount() - 1.0f)
                    {
                        continue;
                    }

                    if (targetDamage > bestDamage)
                    {
                        bestDamage = targetDamage;
                        bestPos = pos;
                    }
                }
            }
        }

        return bestPos;
    }

    protected EntityEnderCrystal findBestCrystal(EntityPlayer enemy)
    {
        EntityEnderCrystal best = null;
        float bestDamage = minDamage.getValue();

        for (Entity entity : mc.world.loadedEntityList)
        {
            if (!(entity instanceof EntityEnderCrystal))
            {
                continue;
            }

            EntityEnderCrystal crystal = (EntityEnderCrystal) entity;
            if (!Managers.SET_DEAD.passedDeathTime(crystal, getDeathTime()))
            {
                continue;
            }

            double distSq = mc.player.getDistanceSq(crystal);
            if (distSq > sq(breakRange.getValue()))
            {
                continue;
            }

            if (raytrace.getValue() && !mc.player.canEntityBeSeen(crystal) && distSq > sq(wallRange.getValue()))
            {
                continue;
            }

            float targetDamage = DamageUtil.calculate(crystal, enemy, mc.world);
            float selfDamage = DamageUtil.calculate(crystal, mc.player, mc.world);
            if (selfDamage > maxSelfDamage.getValue())
            {
                continue;
            }

            if (antiSuicide.getValue() && selfDamage >= mc.player.getHealth() + mc.player.getAbsorptionAmount() - 1.0f)
            {
                continue;
            }

            float required = shouldFacePlace(enemy) ? 1.0f : minDamage.getValue();
            if (targetDamage >= required && targetDamage > bestDamage)
            {
                bestDamage = targetDamage;
                best = crystal;
            }
        }

        return best;
    }

    protected void attackCrystal(EntityEnderCrystal crystal)
    {
        Runnable task = () -> {
            int oldSlot = mc.player.inventory.currentItem;
            int weaknessSlot = -1;

            if (antiWeakness.getValue() && !DamageUtil.canBreakWeakness(true))
            {
                weaknessSlot = DamageUtil.findAntiWeakness();
                if (weaknessSlot == -1)
                {
                    return;
                }

                swapTo(weaknessSlot);
            }

            if (rotate.getValue())
            {
                lookAt(crystal.getPositionVector().add(0.0, 0.5, 0.0));
            }

            PacketUtil.attack(crystal);

            if (pseudoDead.getValue())
            {
                ((IEntity) crystal).setPseudoDead(true);
            }

            if (setDead.getValue())
            {
                Managers.SET_DEAD.setDead(crystal);
            }

            positionCache.markExploded(crystal.getPosition());
            if (endSequenceOnBreak.getValue())
            {
                clearSequence();
            }

            if (weaknessSlot != -1 && switchBack.getValue())
            {
                swapTo(oldSlot);
            }
        };

        Locks.acquire(Locks.PLACE_SWITCH_LOCK, task);
    }

    protected void placeCrystal(BlockPos pos)
    {
        int crystalSlot = InventoryUtil.findHotbarItem(Items.END_CRYSTAL);
        boolean shouldSwitch = autoSwitch.getValue() == AutoSwitchMode.Always
            || autoSwitch.getValue() == AutoSwitchMode.Bind && switching;
        if (!shouldSwitch && crystalSlot != mc.player.inventory.currentItem && crystalSlot != -2)
        {
            return;
        }

        if (crystalSlot == -1)
        {
            return;
        }

        Locks.acquire(Locks.PLACE_SWITCH_LOCK, () -> {
            int oldSlot = mc.player.inventory.currentItem;
            EnumHand hand = EnumHand.MAIN_HAND;
            if (crystalSlot == -2)
            {
                hand = EnumHand.OFF_HAND;
            }
            else if (oldSlot != crystalSlot)
            {
                swapTo(crystalSlot);
            }

            if (rotate.getValue())
            {
                lookAt(new Vec3d(pos).add(0.5, 1.0, 0.5));
            }

            mc.playerController.processRightClickBlock(mc.player, mc.world, pos, EnumFacing.UP,
                    new Vec3d(pos).add(0.5, 1.0, 0.5), hand);

            BlockPos crystalPos = pos.up();
            expecting = pos;
            seqTimer.reset();
            positionCache.pendingPlace(crystalPos);

            if (hand == EnumHand.MAIN_HAND && oldSlot != crystalSlot && switchBack.getValue())
            {
                swapTo(oldSlot);
            }
        });
    }

    protected void swapTo(int slot)
    {
        int old = mc.player.inventory.currentItem;
        if (old == slot)
        {
            return;
        }

        ItemStack[] hotbarCopy = new ItemStack[9];
        for (int i = 0; i < 9; i++)
        {
            hotbarCopy[i] = mc.player.inventory.getStackInSlot(i).copy();
        }

        mc.player.connection.sendPacket(new CPacketHeldItemChange(slot));
        mc.player.inventory.currentItem = slot;
        InventoryUtil.syncItem();
        swapStateTracker.onSwap(old, slot, hotbarCopy);
    }

    protected boolean shouldFacePlace(EntityPlayer enemy)
    {
        if (!facePlace.getValue())
        {
            return false;
        }

        if (enemy.getHealth() + enemy.getAbsorptionAmount() <= facePlaceHealth.getValue())
        {
            return true;
        }

        for (ItemStack stack : enemy.getArmorInventoryList())
        {
            if (!stack.isEmpty() && DamageUtil.getPercent(stack) <= armorBreaker.getValue())
            {
                return true;
            }
        }

        return false;
    }

    protected boolean isValidBase(BlockPos pos)
    {
        Block base = mc.world.getBlockState(pos).getBlock();
        if (base != Blocks.OBSIDIAN && base != Blocks.BEDROCK)
        {
            return false;
        }

        BlockPos up = pos.up();
        BlockPos up2 = up.up();
        return mc.world.isAirBlock(up)
            && mc.world.isAirBlock(up2)
            && mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(up)).isEmpty()
            && mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(up2)).isEmpty();
    }

    protected boolean canPlaceRangeTrace(BlockPos pos)
    {
        Vec3d eyes = mc.player.getPositionEyes(1.0f);
        Vec3d hit = new Vec3d(pos).add(0.5, 1.0, 0.5);
        return mc.world.rayTraceBlocks(eyes, hit, false, true, false) == null;
    }

    protected int getDeathTime()
    {
        float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        return useSafeDeathTime.getValue() && health <= facePlaceHealth.getValue()
            ? safeDeathTime.getValue()
            : deathTime.getValue();
    }

    protected void clearSequence()
    {
        expecting = null;
    }

    protected void lookAt(Vec3d vec)
    {
        double x = vec.x - mc.player.posX;
        double y = vec.y - (mc.player.posY + mc.player.getEyeHeight());
        double z = vec.z - mc.player.posZ;
        double dist = MathHelper.sqrt(x * x + z * z);
        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(y, dist));
        mc.player.connection.sendPacket(new CPacketPlayer.Rotation(yaw, pitch, mc.player.onGround));
    }

    protected boolean canAttackSpawn(BlockPos crystalPos, BlockPos base)
    {
        if (mc.player.getDistanceSq(base) > sq(breakRange.getValue()))
        {
            return false;
        }

        EntityEnderCrystal crystal = new EntityEnderCrystal(mc.world, crystalPos.getX() + 0.5, crystalPos.getY(), crystalPos.getZ() + 0.5);
        double distSq = mc.player.getDistanceSq(crystal);
        if (raytrace.getValue() && !mc.player.canEntityBeSeen(crystal) && distSq > sq(wallRange.getValue()))
        {
            return false;
        }

        if (antiWeakness.getValue() && !DamageUtil.canBreakWeakness(true) && DamageUtil.findAntiWeakness() == -1)
        {
            return false;
        }

        if (target == null)
        {
            return false;
        }

        float selfDamage = DamageUtil.calculate(crystal, mc.player, mc.world);
        float targetDamage = DamageUtil.calculate(crystal, target, mc.world);
        float required = shouldFacePlace(target) ? 1.0f : minDamage.getValue();
        return selfDamage <= maxSelfDamage.getValue()
            && targetDamage >= required
            && (!antiSuicide.getValue()
                || selfDamage < mc.player.getHealth() + mc.player.getAbsorptionAmount() - 1.0f);
    }

    protected static double sq(double d)
    {
        return d * d;
    }

    public enum TargetMode
    {
        Distance,
        Health
    }

    public enum AutoSwitchMode
    {
        None,
        Bind,
        Always
    }
}
