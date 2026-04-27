package me.earth.earthhack.impl.modules.combat.autofeettrap;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.event.events.misc.CollisionEvent;
import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.combat.util.CombatCoordinator;
import me.earth.earthhack.impl.util.client.ModuleUtil;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.minecraft.InventoryUtil;
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
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.network.play.server.SPacketMultiBlockChange;
import net.minecraft.network.play.server.SPacketSpawnObject;
import net.minecraft.util.EnumActionResult;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AutoFeetTrap extends Module
{
    protected final Setting<Mode> mode =
        register(new EnumSetting<>("Mode", Mode.DYNAMIC));

    protected final Setting<BlockMode> blockMode =
        register(new EnumSetting<>("BlockMode", BlockMode.OBSIDIAN_ECHEST));

    protected final Setting<Integer> blocksPerTick =
        register(new NumberSetting<>("BlocksPerTick", 4, 1, 10));

    protected final Setting<Integer> delayTicks =
        register(new NumberSetting<>("DelayTicks", 0, 0, 10));

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

    protected final Setting<Boolean> rotate =
        register(new BooleanSetting("Rotate", true));

    protected final Setting<Boolean> centerPlayer =
        register(new BooleanSetting("CenterPlayer", true));

    protected final Setting<Boolean> packetPlace =
        register(new BooleanSetting("PacketPlace", true));

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

    protected final Setting<Integer> breakDelay =
        register(new NumberSetting<>("BreakDelay", 0, 0, 1000));

    protected final Setting<Float> breakRange =
        register(new NumberSetting<>("BreakRange", 6.0f, 1.0f, 6.0f));

    protected final Setting<Boolean> fakeCollision =
        register(new BooleanSetting("FakeCollision", true));

    protected final Setting<Integer> placedTimeout =
        register(new NumberSetting<>("PlacedTimeout", 250, 50, 1000));

    protected final Setting<Boolean> cooperate =
        register(new BooleanSetting("Cooperate", true));

    protected final Setting<Boolean> sharedAttacks =
        register(new BooleanSetting("SharedAttacks", true));

    protected final Setting<Boolean> avoidCrystalTargets =
        register(new BooleanSetting("AvoidCrystalTargets", false));

    protected final Setting<HandSwing> handSwing =
        register(new EnumSetting<>("HandSwing", HandSwing.PACKET));

    protected final Setting<SwingHand> swingHand =
        register(new EnumSetting<>("SwingHand", SwingHand.AUTO));

    protected final Setting<Boolean> render =
        register(new BooleanSetting("Render", true));

    protected final Setting<RenderMode> renderMode =
        register(new EnumSetting<>("RenderMode", RenderMode.FADE));

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
    protected final Map<BlockPos, Long> renderPositions = new ConcurrentHashMap<>();
    protected final Map<Integer, Long> attackedCrystals = new ConcurrentHashMap<>();

    protected int tickTimer;
    protected BlockPos startPos;
    protected long lastBreak;

    public AutoFeetTrap()
    {
        super("AutoFeetTrap", Category.Combat);

        this.listeners.add(new ListenerTick(this));
        this.listeners.add(new ListenerSpawnObject(this));
        this.listeners.add(new ListenerBlockChange(this));
        this.listeners.add(new ListenerMultiBlockChange(this));
        this.listeners.add(new ListenerCollision(this));
        this.listeners.add(new ListenerRender(this));

        this.setData(new SimpleData(this,
            "Places blocks around your feet with Dynamic/Adaptive logic, Break, Predict, OverPlace, StrictDirection fix and AutoCrystal cooperation.",
            (int) 0xff8fdaffL));
    }

    @Override
    protected void onEnable()
    {
        tickTimer = 0;
        lastBreak = 0L;
        placed.clear();
        renderPositions.clear();
        attackedCrystals.clear();

        if (mc.player != null)
        {
            startPos = getPlayerPos();
        }

        CombatCoordinator.setFeetTrapActive(true);
    }

    @Override
    protected void onDisable()
    {
        tickTimer = 0;
        startPos = null;
        placed.clear();
        renderPositions.clear();
        attackedCrystals.clear();

        CombatCoordinator.setFeetTrapActive(false);
    }

    protected void onTick()
    {
        if (mc.player == null || mc.world == null)
        {
            return;
        }

        cleanCaches();

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

        if (tickTimer++ < delayTicks.getValue())
        {
            return;
        }

        tickTimer = 0;

        centerPlayerIfNeeded();

        int slot = findBlockInHotbar();

        if (slot == -1)
        {
            ModuleUtil.disableRed(this, "Disabled, no valid hotbar block.");
            return;
        }

        List<BlockPos> targets = getSurroundPositions(getPlayerPos());

        if (cooperate.getValue())
        {
            CombatCoordinator.updateFeetTargets(new LinkedHashSet<>(targets));
            CombatCoordinator.setFeetTrapPlacing(!targets.isEmpty());
        }

        if (targets.isEmpty())
        {
            if (cooperate.getValue())
            {
                CombatCoordinator.setFeetTrapPlacing(false);
            }

            return;
        }

        if (breakCrystals.getValue())
        {
            attackBlockingCrystals(targets);
        }

        placeBlocks(targets, slot);

        if (cooperate.getValue())
        {
            CombatCoordinator.setFeetTrapPlacing(false);
        }
    }

    protected BlockPos getPlayerPos()
    {
        return new BlockPos(MathHelper.floor(mc.player.posX),
                            MathHelper.floor(mc.player.posY),
                            MathHelper.floor(mc.player.posZ));
    }

    protected List<BlockPos> getSurroundPositions(BlockPos playerPos)
    {
        Set<BlockPos> targets = new LinkedHashSet<>();
        Set<BlockPos> bases = getBasePositions(playerPos);

        for (BlockPos base : bases)
        {
            for (EnumFacing facing : EnumFacing.HORIZONTALS)
            {
                BlockPos offset = base.offset(facing);

                if (!bases.contains(offset))
                {
                    targets.add(offset);
                }
            }

            if (floor.getValue())
            {
                targets.add(base.down());
            }

            if (mode.getValue() == Mode.FULL)
            {
                addOffsets(targets, base, DIAGONAL_OFFSETS);
            }
        }

        if (antiStep.getValue())
        {
            Set<BlockPos> upper = new LinkedHashSet<>();

            for (BlockPos pos : targets)
            {
                upper.add(pos.up());
            }

            targets.addAll(upper);
        }

        if (support.getValue())
        {
            targets.addAll(getSupportPositions(targets));
        }

        List<BlockPos> list = new ArrayList<>(targets);
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

    protected void placeBlocks(List<BlockPos> targets, int slot)
    {
        Locks.acquire(Locks.PLACE_SWITCH_LOCK, () ->
        {
            int placedThisTick = 0;
            int oldSlot = mc.player.inventory.currentItem;
            boolean switched = false;

            if (slot != -2 && oldSlot != slot)
            {
                if (Managers.SWITCH.getLastSwitch() < 50L)
                {
                    return;
                }

                switchTo(slot);
                switched = true;
            }

            for (BlockPos pos : targets)
            {
                if (placedThisTick >= blocksPerTick.getValue())
                {
                    break;
                }

                if (avoidCrystalTargets.getValue()
                    && CombatCoordinator.getCrystalPlaceTargets().contains(pos))
                {
                    continue;
                }

                if (canPlaceBlock(pos) && placeBlock(pos, InventoryUtil.getHand(slot), slot))
                {
                    long now = System.currentTimeMillis();
                    placed.put(pos, now);
                    renderPositions.put(pos, now);
                    placedThisTick++;
                }
            }

            if (switched)
            {
                switchTo(oldSlot);
            }
        });
    }

    protected boolean canPlaceBlock(BlockPos pos)
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

        Long time = placed.get(pos);

        if (time != null
            && !overPlace.getValue()
            && System.currentTimeMillis() - time < placedTimeout.getValue())
        {
            return false;
        }

        AxisAlignedBB bb = new AxisAlignedBB(pos);

        if (mc.player.getEntityBoundingBox().intersects(bb))
        {
            return false;
        }

        for (Entity entity : mc.world.getEntitiesWithinAABB(Entity.class, bb))
        {
            if (entity == null
                || EntityUtil.isDead(entity)
                || entity == mc.player
                || !entity.preventEntitySpawning)
            {
                continue;
            }

            if (entity instanceof EntityEnderCrystal
                && breakCrystals.getValue()
                && overPlace.getValue())
            {
                if (sharedAttacks.getValue()
                    && CombatCoordinator.wasCrystalRecentlyAttacked(entity.getEntityId(), 750L))
                {
                    continue;
                }

                if (isRecentlyAttacked(entity.getEntityId()) || attackCrystal(entity))
                {
                    continue;
                }

                return false;
            }

            return false;
        }

        return getPlaceData(pos) != null;
    }

    protected boolean placeBlock(BlockPos pos, EnumHand hand, int slot)
    {
        PlaceData data = getPlaceData(pos);

        if (data == null)
        {
            return false;
        }

        if (rotate.getValue())
        {
            faceVector(data.hitVec);
        }

        if (packetPlace.getValue())
        {
            float[] placeVec = hitVecToPlaceVec(data.neighbor, data.hitVec);

            PacketUtil.place(
                data.neighbor,
                data.clickFace,
                hand,
                placeVec[0],
                placeVec[1],
                placeVec[2]
            );

            swing(getSwingHand(swingHand.getValue(), hand));
            return true;
        }

        EnumActionResult result = mc.playerController.processRightClickBlock(
            mc.player,
            mc.world,
            data.neighbor,
            data.clickFace,
            data.hitVec,
            hand
        );

        if (result == EnumActionResult.SUCCESS)
        {
            swing(getSwingHand(swingHand.getValue(), hand));
            return true;
        }

        return false;
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

    protected void attackBlockingCrystals(List<BlockPos> targets)
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
                if (!(entity instanceof EntityEnderCrystal)
                    || EntityUtil.isDead(entity)
                    || mc.player.getDistanceSq(entity.posX, entity.posY, entity.posZ)
                        > square(breakRange.getValue()))
                {
                    continue;
                }

                if (sharedAttacks.getValue()
                    && CombatCoordinator.wasCrystalRecentlyAttacked(entity.getEntityId(), 750L))
                {
                    return;
                }

                attackCrystal(entity);
                return;
            }
        }
    }

    protected boolean attackCrystal(Entity entity)
    {
        if (!breakCrystals.getValue())
        {
            return false;
        }

        if (!canBreakNow())
        {
            return false;
        }

        if (mc.player.getDistanceSq(entity.posX, entity.posY, entity.posZ)
            > square(breakRange.getValue()))
        {
            return false;
        }

        if (rotate.getValue())
        {
            faceVector(entity.getPositionVector().add(0.0, entity.height * 0.5, 0.0));
        }

        mc.player.connection.sendPacket(new CPacketUseEntity(entity));
        swing(EnumHand.MAIN_HAND);

        attackedCrystals.put(entity.getEntityId(), System.currentTimeMillis());
        CombatCoordinator.markCrystalAttacked(entity.getEntityId());
        lastBreak = System.currentTimeMillis();

        return true;
    }

    protected boolean attackCrystal(int id, Vec3d vec)
    {
        if (!breakCrystals.getValue())
        {
            return false;
        }

        if (!canBreakNow())
        {
            return false;
        }

        if (mc.player.getDistanceSq(vec.x, vec.y, vec.z) > square(breakRange.getValue()))
        {
            return false;
        }

        if (rotate.getValue())
        {
            faceVector(vec);
        }

        PacketUtil.attack(id);
        attackedCrystals.put(id, System.currentTimeMillis());
        CombatCoordinator.markCrystalAttacked(id);
        lastBreak = System.currentTimeMillis();

        return true;
    }

    protected boolean canBreakNow()
    {
        return System.currentTimeMillis() - lastBreak >= breakDelay.getValue();
    }

    protected boolean isRecentlyAttacked(int id)
    {
        Long time = attackedCrystals.get(id);
        return time != null && System.currentTimeMillis() - time < 500L;
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
        List<BlockPos> targets = getSurroundPositions(getPlayerPos());

        BlockPos target = null;

        for (BlockPos pos : targets)
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

        int slot = findBlockInHotbar();

        if (slot == -1)
        {
            return;
        }

        Vec3d crystalVec = new Vec3d(packet.getX(), packet.getY(), packet.getZ());

        attackCrystal(packet.getEntityID(), crystalVec);

        List<BlockPos> one = new ArrayList<>();
        one.add(target);

        placeBlocks(one, slot);
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
        if (!centerPlayer.getValue() || mc.player == null)
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
    }

    protected void switchTo(int slot)
    {
        if (mc.player == null
            || slot < 0
            || slot > 8
            || mc.player.inventory.currentItem == slot)
        {
            return;
        }

        mc.player.connection.sendPacket(new CPacketHeldItemChange(slot));
        mc.player.inventory.currentItem = slot;
        InventoryUtil.syncItem();
    }

    protected void faceVector(Vec3d vec)
    {
        double x = vec.x - mc.player.posX;
        double y = vec.y - (mc.player.posY + mc.player.getEyeHeight());
        double z = vec.z - mc.player.posZ;
        double dist = Math.sqrt(x * x + z * z);

        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(y, dist));

        Managers.ROTATION.setBlocking(true);
        mc.player.connection.sendPacket(
            new CPacketPlayer.Rotation(yaw, pitch, mc.player.onGround));
        Managers.ROTATION.setBlocking(false);
    }

    protected float[] hitVecToPlaceVec(BlockPos on, Vec3d hitVec)
    {
        return new float[]
        {
            (float) (hitVec.x - on.getX()),
            (float) (hitVec.y - on.getY()),
            (float) (hitVec.z - on.getZ())
        };
    }

    protected void swing(EnumHand hand)
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

    protected EnumHand getSwingHand(SwingHand setting, EnumHand fallback)
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

    protected void cleanCaches()
    {
        long now = System.currentTimeMillis();

        placed.entrySet().removeIf(e ->
            now - e.getValue() > placedTimeout.getValue());

        renderPositions.entrySet().removeIf(e ->
            now - e.getValue() > Math.max(placedTimeout.getValue(), fadeTime.getValue()));

        attackedCrystals.entrySet().removeIf(e ->
            now - e.getValue() > 500L);

        CombatCoordinator.clean();
    }

    protected void removePlaced(BlockPos pos)
    {
        placed.remove(pos);
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

    private static final class ListenerTick
        extends ModuleListener<AutoFeetTrap, TickEvent>
    {
        private ListenerTick(AutoFeetTrap module)
        {
            super(module, TickEvent.class);
        }

        @Override
        public void invoke(TickEvent event)
        {
            module.onTick();
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
            module.removePlaced(event.getPacket().getBlockPosition());
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
                module.removePlaced(data.getPos());
            }
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

                int fillAlpha = module.renderMode.getValue() == RenderMode.FADE
                    ? MathHelper.clamp(
                        (int) (fill.getAlpha() * (1.0f - age / (float) fade)),
                        0,
                        fill.getAlpha()
                    )
                    : fill.getAlpha();

                int lineAlpha = module.renderMode.getValue() == RenderMode.FADE
                    ? MathHelper.clamp(
                        (int) (line.getAlpha() * (1.0f - age / (float) fade)),
                        0,
                        line.getAlpha()
                    )
                    : line.getAlpha();

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