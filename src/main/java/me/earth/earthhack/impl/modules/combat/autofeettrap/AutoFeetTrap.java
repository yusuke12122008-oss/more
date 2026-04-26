package me.earth.earthhack.impl.modules.combat.autofeettrap;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.util.client.ModuleUtil;
import me.earth.earthhack.impl.util.minecraft.InventoryUtil;
import me.earth.earthhack.impl.util.thread.Locks;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AutoFeetTrap extends Module
{
    protected final Setting<Mode> mode =
        register(new EnumSetting<>("Mode", Mode.NORMAL));
    protected final Setting<BlockMode> blockMode =
        register(new EnumSetting<>("BlockMode", BlockMode.OBSIDIAN));
    protected final Setting<Integer> blocksPerTick =
        register(new NumberSetting<>("BlocksPerTick", 2, 1, 4));
    protected final Setting<Integer> delayTicks =
        register(new NumberSetting<>("DelayTicks", 0, 0, 10));
    protected final Setting<Boolean> onlyOnGround =
        register(new BooleanSetting("OnlyOnGround", true));
    protected final Setting<Boolean> disableOnJump =
        register(new BooleanSetting("DisableOnJump", true));
    protected final Setting<Boolean> disableOnMove =
        register(new BooleanSetting("DisableOnMove", true));
    protected final Setting<Boolean> antiStep =
        register(new BooleanSetting("AntiStep", false));
    protected final Setting<Boolean> support =
        register(new BooleanSetting("Support", true));
    protected final Setting<Boolean> rotate =
        register(new BooleanSetting("Rotate", true));
    protected final Setting<Boolean> centerPlayer =
        register(new BooleanSetting("CenterPlayer", true));

    private static final Vec3d[] NORMAL_OFFSETS = {
        new Vec3d(1, 0, 0),
        new Vec3d(-1, 0, 0),
        new Vec3d(0, 0, 1),
        new Vec3d(0, 0, -1)
    };

    private static final Vec3d[] DIAGONAL_OFFSETS = {
        new Vec3d(1, 0, 1),
        new Vec3d(1, 0, -1),
        new Vec3d(-1, 0, 1),
        new Vec3d(-1, 0, -1)
    };

    protected int tickTimer;
    protected BlockPos startPos;

    public AutoFeetTrap()
    {
        super("AutoFeetTrap", Category.Combat);
        this.listeners.add(new ListenerTick(this));
        this.setData(new AutoFeetTrapData(this));
    }

    @Override
    protected void onEnable()
    {
        tickTimer = 0;
        if (mc.player != null)
        {
            startPos = new BlockPos(mc.player.posX, mc.player.posY, mc.player.posZ);
        }
    }

    @Override
    protected void onDisable()
    {
        tickTimer = 0;
        startPos = null;
    }

    protected void onTick()
    {
        if (mc.player == null || mc.world == null)
        {
            return;
        }

        if (shouldDisable())
        {
            disable();
            return;
        }

        if (onlyOnGround.getValue() && !mc.player.onGround)
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
        if (targets.isEmpty())
        {
            return;
        }

        placeBlocks(targets, slot);
    }

    protected BlockPos getPlayerPos()
    {
        return new BlockPos(MathHelper.floor(mc.player.posX),
                            MathHelper.floor(mc.player.posY),
                            MathHelper.floor(mc.player.posZ));
    }

    protected List<BlockPos> getSurroundPositions(BlockPos playerPos)
    {
        List<BlockPos> targets = new ArrayList<>();
        addOffsets(targets, playerPos, NORMAL_OFFSETS);
        if (mode.getValue() == Mode.FULL)
        {
            addOffsets(targets, playerPos, DIAGONAL_OFFSETS);
        }

        if (antiStep.getValue())
        {
            List<BlockPos> upper = new ArrayList<>();
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

        targets.sort(Comparator.comparingDouble(this::distanceSq));
        return targets;
    }

    private void addOffsets(List<BlockPos> targets, BlockPos playerPos, Vec3d[] offsets)
    {
        for (Vec3d offset : offsets)
        {
            targets.add(playerPos.add(offset.x, offset.y, offset.z));
        }
    }

    protected List<BlockPos> getSupportPositions(List<BlockPos> main)
    {
        List<BlockPos> supports = new ArrayList<>();
        for (BlockPos pos : main)
        {
            if (getPlaceableSide(pos) == null)
            {
                BlockPos down = pos.down();
                if (!main.contains(down) && !supports.contains(down))
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

        if (blockMode.getValue() == BlockMode.OBSIDIAN)
        {
            return InventoryUtil.findHotbarBlock(Blocks.OBSIDIAN);
        }

        if (blockMode.getValue() == BlockMode.ENDER_CHEST)
        {
            return InventoryUtil.findHotbarBlock(Blocks.ENDER_CHEST);
        }

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
        Locks.acquire(Locks.PLACE_SWITCH_LOCK, () -> {
            int placed = 0;
            int oldSlot = mc.player.inventory.currentItem;
            boolean switched = false;

            if (oldSlot != slot)
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
                if (placed >= blocksPerTick.getValue())
                {
                    break;
                }

                if (canPlaceBlock(pos) && placeBlock(pos, EnumHand.MAIN_HAND))
                {
                    placed++;
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

        if (!mc.world.getWorldBorder().contains(pos))
        {
            return false;
        }

        if (!mc.world.getBlockState(pos).getMaterial().isReplaceable())
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
            if (entity != null
                && !entity.isDead
                && entity.preventEntitySpawning
                && entity != mc.player)
            {
                return false;
            }
        }

        return getPlaceableSide(pos) != null;
    }

    protected boolean placeBlock(BlockPos pos, EnumHand hand)
    {
        EnumFacing side = getPlaceableSide(pos);
        if (side == null)
        {
            return false;
        }

        BlockPos neighbor = pos.offset(side);
        EnumFacing opposite = side.getOpposite();
        Vec3d hitVec = new Vec3d(neighbor).add(0.5, 0.5, 0.5)
            .add(new Vec3d(opposite.getDirectionVec()).scale(0.5));

        if (rotate.getValue())
        {
            faceVector(hitVec);
        }

        return mc.playerController.processRightClickBlock(
            mc.player,
            mc.world,
            neighbor,
            opposite,
            hitVec,
            hand
        ) == net.minecraft.util.EnumActionResult.SUCCESS;
    }

    protected EnumFacing getPlaceableSide(BlockPos pos)
    {
        for (EnumFacing side : EnumFacing.values())
        {
            BlockPos neighbor = pos.offset(side);
            IBlockState state = mc.world.getBlockState(neighbor);
            if (!state.getMaterial().isReplaceable())
            {
                return side;
            }
        }

        return null;
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
        if (mc.player == null || mc.player.inventory.currentItem == slot)
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
        mc.player.connection.sendPacket(new CPacketPlayer.Rotation(yaw, pitch, mc.player.onGround));
    }

    private double distanceSq(BlockPos pos)
    {
        return mc.player.getDistanceSq(pos);
    }

    public enum Mode
    {
        NORMAL,
        FULL
    }

    public enum BlockMode
    {
        OBSIDIAN,
        ENDER_CHEST,
        ANY_SOLID
    }
}
