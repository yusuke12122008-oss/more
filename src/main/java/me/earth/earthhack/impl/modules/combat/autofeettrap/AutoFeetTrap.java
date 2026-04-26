package me.earth.earthhack.impl.modules.combat.autofeettrap;

import me.earth.earthhack.api.event.events.Stage;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.event.events.misc.CollisionEvent;
import me.earth.earthhack.impl.event.events.network.MotionUpdateEvent;
import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.client.ModuleUtil;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.helpers.blocks.ObbyModule;
import me.earth.earthhack.impl.util.helpers.blocks.modes.Rotate;
import me.earth.earthhack.impl.util.minecraft.InventoryUtil;
import me.earth.earthhack.impl.util.minecraft.blocks.BlockUtil;
import me.earth.earthhack.impl.util.minecraft.blocks.BlockingType;
import me.earth.earthhack.impl.util.minecraft.entity.EntityUtil;
import me.earth.earthhack.impl.util.network.PacketUtil;
import me.earth.earthhack.impl.util.render.Interpolation;
import me.earth.earthhack.impl.util.render.RenderUtil;
import me.earth.earthhack.impl.util.thread.Locks;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.network.play.server.SPacketExplosion;
import net.minecraft.network.play.server.SPacketMultiBlockChange;
import net.minecraft.network.play.server.SPacketSpawnObject;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AutoFeetTrap extends ObbyModule
{
    protected final Setting<Mode> mode =
        register(new EnumSetting<>("Mode", Mode.DYNAMIC));

    protected final Setting<BlockMode> blockMode =
        register(new EnumSetting<>("BlockMode", BlockMode.OBSIDIAN_ECHEST));

    protected final Setting<Float> placeRange =
        register(new NumberSetting<>("PlaceRange", 5.5f, 1.0f, 6.0f));

    protected final Setting<Boolean> adaptive =
        register(new BooleanSetting("Adaptive", true));

    protected final Setting<Boolean> onlyOnGround =
        register(new BooleanSetting("OnlyOnGround", true));

    protected final Setting<Boolean> disableOnJump =
        register(new BooleanSetting("DisableOnJump", true));

    protected final Setting<Boolean> disableOnMove =
        register(new BooleanSetting("DisableOnMove", true));

    protected final Setting<Boolean> pauseMine =
        register(new BooleanSetting("PauseMine", true));

    protected final Setting<Boolean> antiStep =
        register(new BooleanSetting("AntiStep", false));

    protected final Setting<Boolean> floor =
        register(new BooleanSetting("Floor", false));

    protected final Setting<Boolean> support =
        register(new BooleanSetting("Support", true));

    protected final Setting<Boolean> centerPlayer =
        register(new BooleanSetting("CenterPlayer", true));

    protected final Setting<Boolean> strictDirection =
        register(new BooleanSetting("StrictDirection", false));

    protected final Setting<Boolean> strictFallback =
        register(new BooleanSetting("StrictFallback", true));

    protected final Setting<Boolean> overPlace =
        register(new BooleanSetting("OverPlace", true));

    protected final Setting<Boolean> breakCrystals =
        register(new BooleanSetting("Break", true));

    protected final Setting<Boolean> predict =
        register(new BooleanSetting("Predict", true));

    protected final Setting<Float> breakRange =
        register(new NumberSetting<>("BreakRange", 6.0f, 1.0f, 6.0f));

    protected final Setting<Boolean> fakeCollision =
        register(new BooleanSetting("FakeCollision", true));

    protected final Setting<Integer> placedTimeout =
        register(new NumberSetting<>("PlacedTimeout", 250, 50, 1000));

    protected final Setting<Integer> blockedTimeout =
        register(new NumberSetting<>("BlockedTimeout", 500, 50, 2000));

    protected final Setting<Boolean> render =
        register(new BooleanSetting("Render", true));

    protected final Setting<Integer> fadeTime =
        register(new NumberSetting<>("FadeTime", 300, 0, 2000));

    protected final Setting<Color> fill =
        register(new ColorSetting("Fill", new Color(255, 0, 0, 45)));

    protected final Setting<Color> line =
        register(new ColorSetting("Line", new Color(255, 0, 0, 220)));

    private static final Vec3d[] DIAGONAL_OFFSETS =
    {
        new Vec3d(1, 0, 1),
        new Vec3d(1, 0, -1),
        new Vec3d(-1, 0, 1),
        new Vec3d(-1, 0, -1)
    };

    protected final Map<BlockPos, Long> placed = new ConcurrentHashMap<>();
    protected final Map<BlockPos, Long> confirmed = new ConcurrentHashMap<>();
    protected final Map<BlockPos, Long> recentlyBlocked = new ConcurrentHashMap<>();
    protected final Map<BlockPos, Long> renderPositions = new ConcurrentHashMap<>();
    protected final Map<Integer, Long> attackedCrystals = new ConcurrentHashMap<>();

    protected BlockPos startPos;
    protected Set<BlockPos> targets = new LinkedHashSet<>();
    protected boolean centered;

    public AutoFeetTrap()
    {
        super("AutoFeetTrap", Category.Combat);

        this.listeners.add(new ListenerMotion(this));
        this.listeners.add(new ListenerSpawnObject(this));
        this.listeners.add(new ListenerBlockChange(this));
        this.listeners.add(new ListenerMultiBlockChange(this));
        this.listeners.add(new ListenerExplosion(this));
        this.listeners.add(new ListenerCollision(this));
        this.listeners.add(new ListenerRender(this));

        this.setData(new SimpleData(this,
            "Feet surround with Dynamic/Adaptive logic, ObbyModule placing, Break, Predict, OverPlace, StrictDirection fix and Render.",
            (int) 0xff8fdaffL));
    }

    @Override
    protected void onEnable()
    {
        super.onEnable();

        placed.clear();
        confirmed.clear();
        recentlyBlocked.clear();
        renderPositions.clear();
        attackedCrystals.clear();
        targets.clear();

        centered = false;

        if (mc.player != null)
        {
            startPos = getPlayerPos();
        }
    }

    @Override
    protected void onDisable()
    {
        placed.clear();
        confirmed.clear();
        recentlyBlocked.clear();
        renderPositions.clear();
        attackedCrystals.clear();
        targets.clear();
        startPos = null;
    }

    protected void onMotionPre(MotionUpdateEvent event)
    {
        if (mc.player == null || mc.world == null)
        {
            return;
        }

        cleanCaches();

        rotations = null;
        attacking = null;
        blocksPlaced = 0;

        if (shouldDisable())
        {
            disable();
            return;
        }

        if (onlyOnGround.getValue() && !mc.player.onGround)
        {
            return;
        }

        if (pauseMine.getValue() && isMining())
        {
            return;
        }

        centerPlayerIfNeeded();

        slot = findBlockInHotbar();
        if (slot == -1)
        {
            ModuleUtil.disableRed(this, "Disabled, no valid hotbar block.");
            return;
        }

        targets = new LinkedHashSet<>(getSurroundPositions(getPlayerPos()));
        placed.keySet().retainAll(targets);

        if (targets.isEmpty())
        {
            return;
        }

        placeCrystalBlockingTargetFirst();

        for (BlockPos pos : targets)
        {
            if (blocksPlaced >= blocks.getValue())
            {
                break;
            }

            if (shouldSkipPlaced(pos))
            {
                continue;
            }

            if (!canAttemptPlace(pos))
            {
                continue;
            }

            placeBlock(pos);
        }

        if (rotate.getValue() != Rotate.None && rotations != null)
        {
            Managers.ROTATION.setBlocking(true);
            event.setYaw(rotations[0]);
            event.setPitch(rotations[1]);
        }
    }

    protected void onMotionPost()
    {
        if (mc.player == null || mc.world == null)
        {
            return;
        }

        Locks.acquire(Locks.PLACE_SWITCH_LOCK, this::execute);
        Managers.ROTATION.setBlocking(false);
    }

    protected void placeCrystalBlockingTargetFirst()
    {
        if (!breakCrystals.getValue())
        {
            return;
        }

        for (BlockPos pos : targets)
        {
            AxisAlignedBB bb = new AxisAlignedBB(pos);

            for (Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, bb))
            {
                if (entity instanceof EntityEnderCrystal
                    && !EntityUtil.isDead(entity)
                    && mc.player.getDistanceSq(entity.posX, entity.posY, entity.posZ)
                        <= square(breakRange.getValue()))
                {
                    if (!isRecentlyAttacked(entity.getEntityId()))
                    {
                        attacking = new CPacketUseEntity(entity);
                        attackedCrystals.put(entity.getEntityId(), System.currentTimeMillis());
                    }

                    if (canAttemptPlace(pos))
                    {
                        placeBlock(pos);
                    }

                    return;
                }
            }
        }
    }

    @Override
    public boolean placeBlock(BlockPos pos)
    {
        int before = blocksPlaced;
        boolean stop = super.placeBlock(pos);

        if (blocksPlaced > before)
        {
            long now = System.currentTimeMillis();
            placed.put(pos, now);
            renderPositions.put(pos, now);
        }

        return stop;
    }

    @Override
    public boolean entityCheck(BlockPos pos)
    {
        CPacketUseEntity attackPacket = null;

        for (Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos)))
        {
            if (entity == null
                || EntityUtil.isDead(entity)
                || !entity.preventEntitySpawning
                || entity == mc.player)
            {
                continue;
            }

            if (entity instanceof EntityPlayer
                && !BlockUtil.isBlocking(pos, (EntityPlayer) entity, blockingType.getValue()))
            {
                continue;
            }

            if (entity instanceof EntityEnderCrystal)
            {
                if (blockingType.getValue() == BlockingType.Crystals)
                {
                    continue;
                }

                if (breakCrystals.getValue()
                    && overPlace.getValue()
                    && attackTimer.passed(breakDelay.getValue())
                    && Managers.SWITCH.getLastSwitch() >= cooldown.getValue()
                    && mc.player.getDistanceSq(entity.posX, entity.posY, entity.posZ)
                        <= square(breakRange.getValue()))
                {
                    if (!isRecentlyAttacked(entity.getEntityId()))
                    {
                        attackPacket = new CPacketUseEntity(entity);
                        attackedCrystals.put(entity.getEntityId(), System.currentTimeMillis());
                    }

                    continue;
                }
            }

            return false;
        }

        if (attackPacket != null)
        {
            attacking = attackPacket;
        }

        return true;
    }

    @Override
    protected boolean shouldHelp(EnumFacing facing, BlockPos pos)
    {
        return facing == null || getPlaceData(pos) == null;
    }

    protected boolean canAttemptPlace(BlockPos pos)
    {
        if (mc.player == null || mc.world == null || pos == null)
        {
            return false;
        }

        if (mc.player.getDistanceSq(pos) > square(placeRange.getValue()))
        {
            return false;
        }

        if (!mc.world.getWorldBorder().contains(pos))
        {
            return false;
        }

        if (!mc.world.getBlockState(pos).getMaterial().isReplaceable())
        {
            return false;
        }

        if (mc.player.getEntityBoundingBox().intersects(new AxisAlignedBB(pos)))
        {
            return false;
        }

        return getPlaceData(pos) != null;
    }

    protected boolean shouldSkipPlaced(BlockPos pos)
    {
        Long time = placed.get(pos);

        if (time == null)
        {
            return false;
        }

        if (isRecentlyBlocked(pos))
        {
            placed.remove(pos);
            confirmed.remove(pos);
            return false;
        }

        return !overPlace.getValue()
            && System.currentTimeMillis() - time < placedTimeout.getValue();
    }

    protected boolean isRecentlyBlocked(BlockPos pos)
    {
        Long time = recentlyBlocked.get(pos);
        return time != null
            && System.currentTimeMillis() - time < blockedTimeout.getValue();
    }

    protected BlockPos getPlayerPos()
    {
        return new BlockPos(MathHelper.floor(mc.player.posX),
                            MathHelper.floor(mc.player.posY),
                            MathHelper.floor(mc.player.posZ));
    }

    protected List<BlockPos> getSurroundPositions(BlockPos playerPos)
    {
        Set<BlockPos> result = new LinkedHashSet<>();
        Set<BlockPos> bases = getBasePositions(playerPos);

        for (BlockPos base : bases)
        {
            for (EnumFacing facing : EnumFacing.HORIZONTALS)
            {
                BlockPos offset = base.offset(facing);
                if (!bases.contains(offset))
                {
                    result.add(offset);
                }
            }

            if (floor.getValue())
            {
                result.add(base.down());
            }

            if (mode.getValue() == Mode.FULL)
            {
                addOffsets(result, base, DIAGONAL_OFFSETS);
            }
        }

        if (antiStep.getValue())
        {
            Set<BlockPos> upper = new LinkedHashSet<>();
            for (BlockPos pos : result)
            {
                upper.add(pos.up());
            }

            result.addAll(upper);
        }

        if (support.getValue())
        {
            result.addAll(getSupportPositions(result));
        }

        List<BlockPos> list = new ArrayList<>(result);
        list.sort(Comparator.comparingDouble(this::distanceSq));
        return list;
    }

    protected Set<BlockPos> getBasePositions(BlockPos playerPos)
    {
        Set<BlockPos> bases = new LinkedHashSet<>();

        if (mode.getValue() == Mode.FEET || !adaptive.getValue())
        {
            bases.add(playerPos);
            return bases;
        }

        AxisAlignedBB bb = mc.player.getEntityBoundingBox();

        int minX = MathHelper.floor(bb.minX + 1.0E-4);
        int maxX = MathHelper.floor(bb.maxX - 1.0E-4);
        int minZ = MathHelper.floor(bb.minZ + 1.0E-4);
        int maxZ = MathHelper.floor(bb.maxZ - 1.0E-4);
        int y = MathHelper.floor(bb.minY);

        for (int x = minX; x <= maxX; x++)
        {
            for (int z = minZ; z <= maxZ; z++)
            {
                bases.add(new BlockPos(x, y, z));
            }
        }

        if (bases.isEmpty())
        {
            bases.add(playerPos);
        }

        return bases;
    }

    private void addOffsets(Set<BlockPos> targets, BlockPos playerPos, Vec3d[] offsets)
    {
        for (Vec3d offset : offsets)
        {
            targets.add(playerPos.add((int) offset.x, (int) offset.y, (int) offset.z));
        }
    }

    protected List<BlockPos> getSupportPositions(Set<BlockPos> main)
    {
        List<BlockPos> supports = new ArrayList<>();

        for (BlockPos pos : main)
        {
            if (getPlaceData(pos) == null)
            {
                BlockPos down = pos.down();

                if (!main.contains(down)
                    && !supports.contains(down)
                    && mc.world.getBlockState(down).getMaterial().isReplaceable())
                {
                    supports.add(down);
                }
            }
        }

        return supports;
    }

    protected int findBlockInHotbar()
    {
        if (mc.player == null)
        {
            return -1;
        }

        switch (blockMode.getValue())
        {
            case OBSIDIAN:
                return InventoryUtil.findHotbarBlock(Blocks.OBSIDIAN);

            case ENDER_CHEST:
                return InventoryUtil.findHotbarBlock(Blocks.ENDER_CHEST);

            case OBSIDIAN_ECHEST:
            {
                int slot = InventoryUtil.findHotbarBlock(Blocks.OBSIDIAN);
                return slot == -1
                    ? InventoryUtil.findHotbarBlock(Blocks.ENDER_CHEST)
                    : slot;
            }

            case ANY_SOLID:
            default:
                return findAnySolidBlock();
        }
    }

    protected int findAnySolidBlock()
    {
        for (int i = 8; i >= 0; i--)
        {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);

            if (!(stack.getItem() instanceof ItemBlock))
            {
                continue;
            }

            Block block = ((ItemBlock) stack.getItem()).getBlock();
            IBlockState state = block.getDefaultState();
            Material material = state.getMaterial();

            if (material.isSolid() && state.isFullBlock())
            {
                return i;
            }
        }

        return -1;
    }

    protected PlaceData getPlaceData(BlockPos pos)
    {
        PlaceData strict = getPlaceData(pos, strictDirection.getValue());

        if (strict != null)
        {
            return strict;
        }

        if (strictDirection.getValue() && strictFallback.getValue())
        {
            return getPlaceData(pos, false);
        }

        return null;
    }

    protected PlaceData getPlaceData(BlockPos pos, boolean strict)
    {
        EnumFacing[] order =
        {
            EnumFacing.DOWN,
            EnumFacing.NORTH,
            EnumFacing.SOUTH,
            EnumFacing.EAST,
            EnumFacing.WEST,
            EnumFacing.UP
        };

        for (EnumFacing side : order)
        {
            BlockPos neighbor = pos.offset(side);
            IBlockState state = mc.world.getBlockState(neighbor);

            if (state.getMaterial().isReplaceable())
            {
                continue;
            }

            EnumFacing clickFace = side.getOpposite();

            Vec3d hitVec = new Vec3d(neighbor)
                .add(0.5, 0.5, 0.5)
                .add(new Vec3d(clickFace.getDirectionVec()).scale(0.5));

            if (strict && !canStrictPlace(neighbor, clickFace, hitVec))
            {
                continue;
            }

            return new PlaceData(pos, neighbor, side, clickFace, hitVec);
        }

        return null;
    }

    protected boolean canStrictPlace(BlockPos neighbor, EnumFacing clickFace, Vec3d hitVec)
    {
        if (mc.player == null || mc.world == null)
        {
            return false;
        }

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
            && result.getBlockPos().equals(neighbor)
            && result.sideHit == clickFace)
        {
            return true;
        }

        return isOnVisibleSide(eyes, neighbor, clickFace);
    }

    protected boolean isOnVisibleSide(Vec3d eyes, BlockPos pos, EnumFacing face)
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

    protected void onCrystalSpawn(SPacketSpawnObject packet)
    {
        if (!predict.getValue()
            || !breakCrystals.getValue()
            || mc.player == null
            || mc.world == null
            || packet.getType() != 51)
        {
            return;
        }

        EntityEnderCrystal crystal =
            new EntityEnderCrystal(mc.world,
                                    packet.getX(),
                                    packet.getY(),
                                    packet.getZ());

        AxisAlignedBB crystalBB = crystal.getEntityBoundingBox();

        List<BlockPos> currentTargets = getSurroundPositions(getPlayerPos());
        BlockPos target = null;

        for (BlockPos pos : currentTargets)
        {
            if (crystalBB.intersects(new AxisAlignedBB(pos)))
            {
                target = pos;
                break;
            }
        }

        if (target == null)
        {
            return;
        }

        slot = findBlockInHotbar();
        if (slot == -1)
        {
            return;
        }

        if (!isRecentlyAttacked(packet.getEntityID()))
        {
            Vec3d crystalVec = new Vec3d(packet.getX(), packet.getY(), packet.getZ());

            if (mc.player.getDistanceSq(crystalVec.x, crystalVec.y, crystalVec.z)
                <= square(breakRange.getValue()))
            {
                if (rotate.getValue() != Rotate.None)
                {
                    float[] r = getRotations(crystalVec);
                    PacketUtil.doRotation(r[0], r[1], mc.player.onGround);
                }

                PacketUtil.attack(packet.getEntityID());
                attackedCrystals.put(packet.getEntityID(), System.currentTimeMillis());
                attackTimer.reset();
            }
        }

        if (!canAttemptPlace(target))
        {
            return;
        }

        int before = blocksPlaced;
        placeBlock(target);

        if (blocksPlaced > before)
        {
            Locks.acquire(Locks.PLACE_SWITCH_LOCK, this::execute);
        }
    }

    protected boolean shouldDisable()
    {
        if (mc.player == null)
        {
            return false;
        }

        if (disableOnJump.getValue() && mc.player.motionY > 0.2)
        {
            ModuleUtil.disableRed(this, "Disabled, jump detected.");
            return true;
        }

        if (disableOnMove.getValue() && startPos != null)
        {
            double movedSq = startPos.distanceSq(getPlayerPos());

            if (movedSq > 0.25)
            {
                ModuleUtil.disableRed(this, "Disabled, movement detected.");
                return true;
            }
        }

        return false;
    }

    protected boolean isMining()
    {
        return mc.gameSettings.keyBindAttack.isKeyDown()
            && mc.objectMouseOver != null
            && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK;
    }

    protected void centerPlayerIfNeeded()
    {
        if (!centerPlayer.getValue() || mc.player == null || centered)
        {
            return;
        }

        double cx = Math.floor(mc.player.posX) + 0.5;
        double cz = Math.floor(mc.player.posZ) + 0.5;
        double dx = cx - mc.player.posX;
        double dz = cz - mc.player.posZ;

        if (Math.abs(dx) < 0.2 && Math.abs(dz) < 0.2)
        {
            mc.player.motionX += dx * 0.15;
            mc.player.motionZ += dz * 0.15;
        }
        else
        {
            centered = true;
        }
    }

    protected void onBlockChange(BlockPos pos, IBlockState state)
    {
        if (!targets.contains(pos) && !placed.containsKey(pos))
        {
            return;
        }

        if (state.getMaterial().isReplaceable())
        {
            confirmed.remove(pos);
            recentlyBlocked.put(pos, System.currentTimeMillis());
        }
        else
        {
            confirmed.put(pos, System.currentTimeMillis());
            placed.remove(pos);
        }
    }

    protected void onExplosion(List<BlockPos> affected)
    {
        long now = System.currentTimeMillis();

        for (BlockPos pos : affected)
        {
            if (targets.contains(pos) || placed.containsKey(pos) || confirmed.containsKey(pos))
            {
                confirmed.remove(pos);
                placed.remove(pos);
                recentlyBlocked.put(pos, now);
            }
        }
    }

    protected boolean isRecentlyAttacked(int id)
    {
        Long time = attackedCrystals.get(id);
        return time != null && System.currentTimeMillis() - time < 500L;
    }

    protected void cleanCaches()
    {
        long now = System.currentTimeMillis();

        placed.entrySet().removeIf(e ->
            now - e.getValue() > placedTimeout.getValue());

        confirmed.entrySet().removeIf(e ->
            now - e.getValue() > 5000L);

        recentlyBlocked.entrySet().removeIf(e ->
            now - e.getValue() > blockedTimeout.getValue());

        renderPositions.entrySet().removeIf(e ->
            now - e.getValue() > Math.max(placedTimeout.getValue(), fadeTime.getValue()));

        attackedCrystals.entrySet().removeIf(e ->
            now - e.getValue() > 500L);
    }

    protected float[] getRotations(Vec3d vec)
    {
        double x = vec.x - mc.player.posX;
        double y = vec.y - (mc.player.posY + mc.player.getEyeHeight());
        double z = vec.z - mc.player.posZ;
        double dist = Math.sqrt(x * x + z * z);

        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(y, dist));

        return new float[]
        {
            yaw,
            pitch
        };
    }

    private double distanceSq(BlockPos pos)
    {
        return mc.player.getDistanceSq(pos);
    }

    private double square(double value)
    {
        return value * value;
    }

    protected static final class PlaceData
    {
        final BlockPos target;
        final BlockPos neighbor;
        final EnumFacing placeSide;
        final EnumFacing clickFace;
        final Vec3d hitVec;

        PlaceData(BlockPos target,
                  BlockPos neighbor,
                  EnumFacing placeSide,
                  EnumFacing clickFace,
                  Vec3d hitVec)
        {
            this.target = target;
            this.neighbor = neighbor;
            this.placeSide = placeSide;
            this.clickFace = clickFace;
            this.hitVec = hitVec;
        }
    }

    public enum Mode
    {
        FEET,
        DYNAMIC,
        FULL
    }

    public enum BlockMode
    {
        OBSIDIAN,
        ENDER_CHEST,
        OBSIDIAN_ECHEST,
        ANY_SOLID
    }

    private static final class ListenerMotion
        extends ModuleListener<AutoFeetTrap, MotionUpdateEvent>
    {
        private ListenerMotion(AutoFeetTrap module)
        {
            super(module, MotionUpdateEvent.class, -999999999);
        }

        @Override
        public void invoke(MotionUpdateEvent event)
        {
            if (event.getStage() == Stage.PRE)
            {
                module.onMotionPre(event);
            }
            else
            {
                module.onMotionPost();
            }
        }
    }

    private static final class ListenerSpawnObject
        extends ModuleListener<AutoFeetTrap, PacketEvent.Receive<SPacketSpawnObject>>
    {
        private ListenerSpawnObject(AutoFeetTrap module)
        {
            super(module, PacketEvent.Receive.class, SPacketSpawnObject.class);
        }

        @Override
        public void invoke(PacketEvent.Receive<SPacketSpawnObject> event)
        {
            module.onCrystalSpawn(event.getPacket());
        }
    }

    private static final class ListenerBlockChange
        extends ModuleListener<AutoFeetTrap, PacketEvent.Receive<SPacketBlockChange>>
    {
        private ListenerBlockChange(AutoFeetTrap module)
        {
            super(module, PacketEvent.Receive.class, SPacketBlockChange.class);
        }

        @Override
        public void invoke(PacketEvent.Receive<SPacketBlockChange> event)
        {
            SPacketBlockChange packet = event.getPacket();
            module.onBlockChange(packet.getBlockPosition(), packet.getBlockState());
        }
    }

    private static final class ListenerMultiBlockChange
        extends ModuleListener<AutoFeetTrap, PacketEvent.Receive<SPacketMultiBlockChange>>
    {
        private ListenerMultiBlockChange(AutoFeetTrap module)
        {
            super(module, PacketEvent.Receive.class, SPacketMultiBlockChange.class);
        }

        @Override
        public void invoke(PacketEvent.Receive<SPacketMultiBlockChange> event)
        {
            for (SPacketMultiBlockChange.BlockUpdateData data
                : event.getPacket().getChangedBlocks())
            {
                module.onBlockChange(data.getPos(), data.getBlockState());
            }
        }
    }

    private static final class ListenerExplosion
        extends ModuleListener<AutoFeetTrap, PacketEvent.Receive<SPacketExplosion>>
    {
        private ListenerExplosion(AutoFeetTrap module)
        {
            super(module, PacketEvent.Receive.class, SPacketExplosion.class);
        }

        @Override
        public void invoke(PacketEvent.Receive<SPacketExplosion> event)
        {
            module.onExplosion(event.getPacket().getAffectedBlockPositions());
        }
    }

    private static final class ListenerCollision
        extends ModuleListener<AutoFeetTrap, CollisionEvent>
    {
        private ListenerCollision(AutoFeetTrap module)
        {
            super(module, CollisionEvent.class);
        }

        @Override
        public void invoke(CollisionEvent event)
        {
            if (!module.fakeCollision.getValue()
                || mc.player == null
                || event.getEntity() != mc.player
                || !module.placed.containsKey(event.getPos()))
            {
                return;
            }

            event.setBB(new AxisAlignedBB(event.getPos()));
        }
    }

    private static final class ListenerRender
        extends ModuleListener<AutoFeetTrap, Render3DEvent>
    {
        private ListenerRender(AutoFeetTrap module)
        {
            super(module, Render3DEvent.class);
        }

        @Override
        public void invoke(Render3DEvent event)
        {
            if (!module.render.getValue()
                || mc.player == null
                || mc.world == null)
            {
                return;
            }

            long now = System.currentTimeMillis();
            int fade = Math.max(1, module.fadeTime.getValue());

            for (Map.Entry<BlockPos, Long> entry : module.renderPositions.entrySet())
            {
                long age = now - entry.getValue();

                if (age > fade)
                {
                    continue;
                }

                Color fill = module.fill.getValue();
                Color line = module.line.getValue();

                int fillAlpha = MathHelper.clamp(
                    (int) (fill.getAlpha() * (1.0f - age / (float) fade)),
                    0,
                    fill.getAlpha()
                );

                int lineAlpha = MathHelper.clamp(
                    (int) (line.getAlpha() * (1.0f - age / (float) fade)),
                    0,
                    line.getAlpha()
                );

                RenderUtil.renderBox(
                    Interpolation.interpolatePos(entry.getKey(), 1.0f),
                    new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), fillAlpha),
                    new Color(line.getRed(), line.getGreen(), line.getBlue(), lineAlpha),
                    1.5f
                );
            }
        }
    }
}