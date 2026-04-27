package me.earth.earthhack.impl.modules.misc.fastbreak;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.api.setting.settings.NumberSetting;
import me.earth.earthhack.impl.event.events.misc.ClickBlockEvent;
import me.earth.earthhack.impl.event.events.misc.DamageBlockEvent;
import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.events.render.Render3DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.managers.Managers;
import me.earth.earthhack.impl.modules.combat.util.CombatCoordinator;
import me.earth.earthhack.impl.util.client.SimpleData;
import me.earth.earthhack.impl.util.minecraft.InventoryUtil;
import me.earth.earthhack.impl.util.minecraft.entity.EntityUtil;
import me.earth.earthhack.impl.util.render.Interpolation;
import me.earth.earthhack.impl.util.render.RenderUtil;
import me.earth.earthhack.impl.util.thread.Locks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.network.play.server.SPacketMultiBlockChange;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FastBreak extends Module
{
    public static FastBreak INSTANCE;

    protected final Setting<Page> page =
        register(new EnumSetting<>("Page", Page.BREAKING));

    protected final Setting<Float> breakAt =
        register(new NumberSetting<>("BreakAt", 1.0f, 0.0f, 1.0f));

    protected final Setting<BreakMode> mode =
        register(new EnumSetting<>("Type", BreakMode.NORMAL));

    protected final Setting<DoubleLogic> logic =
        register(new EnumSetting<>("Logic", DoubleLogic.ALWAYS));

    protected final Setting<Float> breakRange =
        register(new NumberSetting<>("Range", 6.0f, 1.0f, 6.0f));

    protected final Setting<Float> wallsRange =
        register(new NumberSetting<>("Walls", 3.0f, 0.0f, 6.0f));

    protected final Setting<SwapMode> swapMode =
        register(new EnumSetting<>("Switch", SwapMode.SILENT));

    protected final Setting<Boolean> strictDirection =
        register(new BooleanSetting("Strict", true));

    protected final Setting<Boolean> strictFallback =
        register(new BooleanSetting("StrictFallback", true));

    protected final Setting<Boolean> airStrict =
        register(new BooleanSetting("AirStrict", false));

    protected final Setting<Boolean> rotate =
        register(new BooleanSetting("Rotate", false));

    protected final Setting<Boolean> builders =
        register(new BooleanSetting("2b2t", false));

    protected final Setting<Boolean> inhibit =
        register(new BooleanSetting("Inhibit", false));

    protected final Setting<Boolean> swing =
        register(new BooleanSetting("Swing", true));

    protected final Setting<Boolean> unbreak =
        register(new BooleanSetting("Unbreak", true));

    protected final Setting<RebreakMode> rebreak =
        register(new EnumSetting<>("Rebreak", RebreakMode.NONE));

    protected final Setting<Boolean> predict =
        register(new BooleanSetting("Predict", true));

    protected final Setting<Integer> predictDelay =
        register(new NumberSetting<>("Delay", 0, 0, 1000));

    protected final Setting<PauseMode> pause =
        register(new EnumSetting<>("Pause", PauseMode.NONE));

    protected final Setting<Boolean> autoCity =
        register(new BooleanSetting("AutoCity", true));

    protected final Setting<SelectionMode> selectionMode =
        register(new EnumSetting<>("Select", SelectionMode.SMART));

    protected final Setting<Boolean> preferFastMine =
        register(new BooleanSetting("PreferFast", true));

    protected final Setting<Boolean> preferCrystalOpen =
        register(new BooleanSetting("PreferCrystal", true));

    protected final Setting<Boolean> avoidSelfTrap =
        register(new BooleanSetting("AvoidSelfTrap", true));

    protected final Setting<Boolean> avoidFeetTrapTargets =
        register(new BooleanSetting("AvoidFeetTrap", true));

    protected final Setting<Boolean> preferVisible =
        register(new BooleanSetting("PreferVisible", true));

    protected final Setting<Boolean> preferLowY =
        register(new BooleanSetting("PreferLowY", true));

    protected final Setting<Float> targetRange =
        register(new NumberSetting<>("Distance", 7.0f, 3.0f, 20.0f));

    protected final Setting<Boolean> antiCrawl =
        register(new BooleanSetting("AntiCrawl", true));

    protected final Setting<Boolean> preMine =
        register(new BooleanSetting("PreMine", true));

    protected final Setting<Boolean> selfMine =
        register(new BooleanSetting("SelfMine", false));

    protected final Setting<Boolean> headCrystal =
        register(new BooleanSetting("HeadCrystal", false));

    protected final Setting<Boolean> pauseCombatPlace =
        register(new BooleanSetting("PauseCombatPlace", true));

    protected final Setting<Boolean> lockSwitch =
        register(new BooleanSetting("LockSwitch", true));

    protected final Setting<Boolean> packetSwing =
        register(new BooleanSetting("PacketSwing", true));

    protected final Setting<Boolean> render =
        register(new BooleanSetting("Render", true));

    protected final Setting<Color> fillColor =
        register(new ColorSetting("Fill", new Color(255, 0, 0, 81)));

    protected final Setting<Color> lineColor =
        register(new ColorSetting("Line", new Color(255, 0, 0, 255)));

    protected final Setting<Color> fillColor2 =
        register(new ColorSetting("Fill2", new Color(0, 255, 0, 81)));

    protected final Setting<Color> lineColor2 =
        register(new ColorSetting("Line2", new Color(0, 255, 0, 255)));

    protected final Setting<Integer> timeout =
        register(new NumberSetting<>("Timeout", 6000, 1000, 10000));

    protected final Setting<Integer> mineTimeout =
        register(new NumberSetting<>("MineTimeout", 500, 50, 3000));

    protected final Setting<Integer> cooldown =
        register(new NumberSetting<>("Cooldown", 150, 0, 1000));

    protected final Setting<Integer> unbreakCooldown =
        register(new NumberSetting<>("UnbreakCooldown", 150, 0, 1000));

    public BreakData normalData;
    public BreakData packetData;

    public boolean didAction;
    public BlockPos selectedPos;

    private long lastNormalPacket;
    private long lastPacketBreak;
    private long lastUnbreak;
    private long lastPredict;
    private long lastBreak;

    private boolean switchBack;
    private boolean longSwitchBack;
    private int switchBackSlot;

    private BreakRequest mainQueue;
    private BreakRequest packetQueue;

    private String autoCityInfo;

    public FastBreak()
    {
        super("FastBreak", Category.Misc);

        INSTANCE = this;

        this.listeners.add(new ListenerTick(this));
        this.listeners.add(new ListenerPostTick(this));
        this.listeners.add(new ListenerClickBlock(this));
        this.listeners.add(new ListenerDamageBlock(this));
        this.listeners.add(new ListenerPacketSend(this));
        this.listeners.add(new ListenerBlockChange(this));
        this.listeners.add(new ListenerMultiBlockChange(this));
        this.listeners.add(new ListenerRender(this));

        this.setData(new SimpleData(this,
            "Sn0w AutoBreak inspired packet mining with smarter AutoCity selection.",
            0xffff5555));
    }

    @Override
    protected void onEnable()
    {
        normalData = null;
        packetData = null;
        selectedPos = null;
        mainQueue = null;
        packetQueue = null;
        autoCityInfo = null;
        didAction = false;
        switchBack = false;
        longSwitchBack = false;
    }

    @Override
    protected void onDisable()
    {
        if (normalData != null && normalData.isMining())
        {
            abort(normalData);
        }

        if (packetData != null && packetData.isMining())
        {
            abort(packetData);
        }

        normalData = null;
        packetData = null;
        selectedPos = null;
        mainQueue = null;
        packetQueue = null;
        autoCityInfo = null;
        didAction = false;
        switchBack = false;
        longSwitchBack = false;
    }

    @Override
    public String getDisplayInfo()
    {
        if (normalData != null && normalData.isMining())
        {
            return mode.getValue().name()
                + " "
                + Math.round(normalData.bestDamage * 100.0f)
                + "%"
                + (autoCityInfo == null ? "" : " " + autoCityInfo);
        }

        if (packetData != null && packetData.isMining())
        {
            return "Packet "
                + Math.round(packetData.bestDamage * 100.0f)
                + "%"
                + (autoCityInfo == null ? "" : " " + autoCityInfo);
        }

        return autoCityInfo;
    }

    protected void onTick()
    {
        if (mc.player == null || mc.world == null)
        {
            return;
        }

        if (shouldPauseForCombat())
        {
            didAction = false;
            return;
        }

        handleSwitchBack();
        tickData();

        if (autoCity.getValue())
        {
            calcAutoCity();
        }

        if (packetQueue != null)
        {
            startPacketMining(packetQueue.pos, packetQueue.facing, true);
            packetQueue = null;
        }
        else if (mainQueue != null)
        {
            if (isMining())
            {
                abort(normalData);
            }

            startNormalMining(mainQueue.pos, mainQueue.facing, true);

            if (mainQueue.reset)
            {
                normalData.resetOverride = true;
            }

            mainQueue = null;
        }

        if (selectedPos != null
            && !isMining()
            && rebreak.getValue() != RebreakMode.NONE
            && rebreak.getValue() != RebreakMode.INSTA
            && !isAir(selectedPos)
            && passed(lastNormalPacket, cooldown.getValue()))
        {
            if (distanceTo(selectedPos) <= breakRange.getValue())
            {
                EnumFacing side = getMineableSide(selectedPos);

                if (side != null)
                {
                    startNormalMining(selectedPos, side, false);
                    lastNormalPacket = System.currentTimeMillis();
                }
            }
            else
            {
                selectedPos = null;
            }
        }
    }

    protected void onPostTick()
    {
        if (mc.player == null || mc.world == null)
        {
            return;
        }

        didAction = false;
    }

    protected void onClickBlock(ClickBlockEvent event)
    {
        if (mc.player == null || mc.world == null || mc.player.isCreative())
        {
            return;
        }

        event.setCancelled(true);

        if (!canMine(event.getPos()))
        {
            if (isMining(event.getPos())
                && unbreak.getValue()
                && passed(lastUnbreak, unbreakCooldown.getValue()))
            {
                abort(normalData);
                selectedPos = null;
                lastUnbreak = System.currentTimeMillis();
            }

            return;
        }

        if (unbreak.getValue()
            && !passed(lastUnbreak, unbreakCooldown.getValue()))
        {
            return;
        }

        doSwing(EnumHand.MAIN_HAND);
        doManualMine(event.getPos(), event.getFacing());
        lastUnbreak = System.currentTimeMillis();
    }

    protected void onDamageBlock(DamageBlockEvent event)
    {
        if (mc.player == null || mc.world == null)
        {
            return;
        }

        event.setDelay(0);

        if (isMining(event.getPos()) || isPacketMining(event.getPos()))
        {
            event.setDamage(Math.max(event.getDamage(), breakAt.getValue()));
        }
    }

    protected void onPacketSend(PacketEvent.Send<?> event)
    {
        if (mc.player == null || mc.world == null)
        {
            return;
        }

        if (event.getPacket() instanceof CPacketHeldItemChange)
        {
            if (strictDirection.getValue()
                && swapMode.getValue() == SwapMode.ALTER
                && isMining())
            {
                abort(normalData);
            }
        }
    }

    protected void onBlockChange(BlockPos pos, IBlockState state)
    {
        if (isAnyMining(pos))
        {
            if (isPacketMining(pos))
            {
                packetData.updateBlock(state);
            }
            else if (isMining(pos))
            {
                normalData.updateBlock(state);
            }
        }
    }

    protected void tickData()
    {
        if (isMining())
        {
            normalData.tick();

            if (normalData.timeout())
            {
                abort(normalData);
            }

            if (normalData.isReady(false) && canDoBreak(normalData))
            {
                doNormalBreak();
            }
        }

        if (isPacketMining())
        {
            packetData.tick();

            if (packetData.timeout())
            {
                packetData.reset();
            }

            if (packetData.isReady(true) && canDoBreak(packetData))
            {
                doPacketBreak();
            }
        }
    }

    protected boolean canDoBreak(BreakData data)
    {
        if (data == packetData
            || rebreak.getValue() == RebreakMode.NONE
            || rebreak.getValue() == RebreakMode.NORMAL
            || rebreak.getValue() == RebreakMode.FAST
            || data.resetOverride)
        {
            if (data == packetData && data.attempts > 0)
            {
                if (!passed(lastPacketBreak, cooldown.getValue()))
                {
                    return false;
                }
            }

            if (data.isAir())
            {
                data.reset();

                if (data == normalData)
                {
                    abort(normalData);
                }

                return false;
            }
        }
        else if (data == normalData)
        {
            if (normalData.attempts < 1 && isBlockDelayGrim())
            {
                return false;
            }

            if (data.isAir()
                && rebreak.getValue() == RebreakMode.INSTA
                && predict.getValue()
                && passed(lastPredict, predictDelay.getValue()))
            {
                lastPredict = System.currentTimeMillis();
                return true;
            }

            return !data.isAir();
        }

        return true;
    }

    protected void doNormalBreak()
    {
        if (pause.getValue() == PauseMode.EAT && mc.player.isHandActive())
        {
            return;
        }

        BlockPos pos = normalData.pos;
        EnumFacing facing = getMineableSide(pos);

        if (facing == null)
        {
            abort(normalData);
            return;
        }

        if (!inRange(pos, facing))
        {
            abort(normalData);
            return;
        }

        int slot = normalData.getBestSlot();
        int oldSlot = mc.player.inventory.currentItem;

        boolean doSwap = slot != -1 && oldSlot != slot;

        if (doSwap && swapMode.getValue() == SwapMode.NONE)
        {
            abort(normalData);
            return;
        }

        if (rotate.getValue())
        {
            rotateTo(pos, facing);
        }

        doBreakWithLock(normalData, facing, slot, oldSlot);
    }

    protected void doBreakWithLock(BreakData data,
                                   EnumFacing facing,
                                   int slot,
                                   int oldSlot)
    {
        Runnable runnable = () ->
        {
            boolean doSwap = slot != -1 && oldSlot != slot;

            if (doSwap)
            {
                switch (swapMode.getValue())
                {
                    case NONE:
                        abort(data);
                        return;

                    case SILENT:
                    case ALTER:
                        switchTo(slot);
                        didAction = true;
                        break;

                    case SIX_B6T:
                        switchBackSlot = oldSlot;
                        switchTo(slot);
                        switchBack = true;
                        didAction = true;
                        break;
                }
            }

            data.markAttempt();

            sendStopAbort(data.pos, facing);
            lastBreak = System.currentTimeMillis();

            if (doSwap)
            {
                switch (swapMode.getValue())
                {
                    case SILENT:
                    case ALTER:
                        switchTo(oldSlot);
                        break;
                    default:
                        break;
                }
            }

            if (rebreak.getValue() == RebreakMode.INSTA || data.resetOverride)
            {
                return;
            }

            data.reset();

            if (rebreak.getValue() == RebreakMode.FAST)
            {
                startNormalMining(data.pos, facing, false);
            }
        };

        if (lockSwitch.getValue())
        {
            Locks.acquire(Locks.PLACE_SWITCH_LOCK, runnable);
        }
        else
        {
            runnable.run();
        }
    }

    protected void doPacketBreak()
    {
        if (pause.getValue() == PauseMode.EAT && mc.player.isHandActive())
        {
            return;
        }

        BlockPos pos = packetData.pos;
        EnumFacing facing = packetData.facing;

        if (distanceTo(pos) > breakRange.getValue())
        {
            packetData.reset();
            return;
        }

        int slot = packetData.getBestSlot();
        int oldSlot = mc.player.inventory.currentItem;

        boolean doSwap = slot != -1 && oldSlot != slot;

        Runnable runnable = () ->
        {
            if (doSwap)
            {
                switch (swapMode.getValue())
                {
                    case NONE:
                        packetData.reset();
                        return;

                    case SILENT:
                    case SIX_B6T:
                        switchTo(slot);
                        switchBackSlot = oldSlot;
                        switchBack = true;

                        if (swapMode.getValue() == SwapMode.SIX_B6T)
                        {
                            longSwitchBack = true;
                        }

                        didAction = true;
                        break;

                    case ALTER:
                        switchTo(slot);
                        switchBackSlot = oldSlot;
                        switchBack = true;
                        didAction = true;
                        break;
                }
            }

            packetData.markAttempt();
            lastPacketBreak = System.currentTimeMillis();

            if (packetData.attempts > 2)
            {
                packetData.reset();
            }
        };

        if (lockSwitch.getValue())
        {
            Locks.acquire(Locks.PLACE_SWITCH_LOCK, runnable);
        }
        else
        {
            runnable.run();
        }
    }

    public void doManualMine(BlockPos pos, EnumFacing facing)
    {
        selectedPos = pos;
        autoCityInfo = "Manual";

        switch (mode.getValue())
        {
            case NORMAL:
                if (isMining())
                {
                    abort(normalData);
                }

                startNormalMining(pos, facing, false);
                break;

            case DOUBLE:
                if (logic.getValue() == DoubleLogic.ALWAYS && canPacketMine())
                {
                    startPacketMining(pos, facing, false);
                    return;
                }

                if (isMining())
                {
                    abort(normalData);
                }

                if ((logic.getValue() != DoubleLogic.ONLY
                    || mc.world.getBlockState(pos).getBlock() == Blocks.ENDER_CHEST)
                    && canPacketMine())
                {
                    startPacketMining(pos, facing, false);
                }
                else
                {
                    startNormalMining(pos, facing, false);
                }

                break;
        }
    }

    protected void calcAutoCity()
    {
        autoCityInfo = null;

        if (isMining() || isPacketMining())
        {
            return;
        }

        EntityPlayer target = findTarget();

        if (target == null)
        {
            return;
        }

        List<CityCandidate> candidates = getSmartCityCandidates(target);

        if (candidates.isEmpty())
        {
            return;
        }

        CityCandidate best = candidates.get(0);

        selectedPos = best.pos;
        autoCityInfo = best.reason;

        if (mode.getValue() == BreakMode.DOUBLE && canPacketMine())
        {
            packetQueue = new BreakRequest(best.pos, best.facing, false);
        }
        else
        {
            mainQueue = new BreakRequest(best.pos, best.facing, false);
        }
    }

    protected List<CityCandidate> getSmartCityCandidates(EntityPlayer player)
    {
        Set<BlockPos> raw = new LinkedHashSet<>();
        List<CityCandidate> candidates = new ArrayList<>();

        BlockPos base = new BlockPos(
            MathHelper.floor(player.posX),
            MathHelper.floor(player.posY),
            MathHelper.floor(player.posZ)
        );

        addRaw(raw, base.north());
        addRaw(raw, base.south());
        addRaw(raw, base.east());
        addRaw(raw, base.west());
        addRaw(raw, base);

        if (antiCrawl.getValue())
        {
            addRaw(raw, base.up());
            addRaw(raw, base.up(2));
            addRaw(raw, base.up().north());
            addRaw(raw, base.up().south());
            addRaw(raw, base.up().east());
            addRaw(raw, base.up().west());
        }

        if (headCrystal.getValue())
        {
            addRaw(raw, base.up(2).north());
            addRaw(raw, base.up(2).south());
            addRaw(raw, base.up(2).east());
            addRaw(raw, base.up(2).west());
        }

        if (selfMine.getValue())
        {
            BlockPos self = new BlockPos(
                MathHelper.floor(mc.player.posX),
                MathHelper.floor(mc.player.posY),
                MathHelper.floor(mc.player.posZ)
            );

            addRaw(raw, self.north());
            addRaw(raw, self.south());
            addRaw(raw, self.east());
            addRaw(raw, self.west());
        }

        for (BlockPos pos : raw)
        {
            CityCandidate candidate = evaluateCityCandidate(player, pos);

            if (candidate != null)
            {
                candidates.add(candidate);
            }
        }

        candidates.sort((a, b) -> Double.compare(b.score, a.score));
        return candidates;
    }

    private void addRaw(Set<BlockPos> set, BlockPos pos)
    {
        if (pos != null)
        {
            set.add(pos);
        }
    }

    protected CityCandidate evaluateCityCandidate(EntityPlayer target, BlockPos pos)
    {
        if (pos == null || mc.world == null || mc.player == null)
        {
            return null;
        }

        if (avoidFeetTrapTargets.getValue()
            && CombatCoordinator.isFeetTarget(pos))
        {
            return null;
        }

        if (!canMine(pos))
        {
            return null;
        }

        EnumFacing side = getMineableSide(pos);

        if (side == null)
        {
            return null;
        }

        if (!inRange(pos, side))
        {
            return null;
        }

        IBlockState state = mc.world.getBlockState(pos);

        if (!isMineable(state, pos))
        {
            return null;
        }

        if (!preMine.getValue() && !touchesTargetBox(target, pos))
        {
            return null;
        }

        double score = 0.0;
        String reason = "Smart";

        double playerDistance = distanceTo(pos);
        double targetDistance = target.getDistance(
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5
        );

        float mineSpeed = estimateBestDamage(pos, state);
        boolean visible = canSeeMineSide(pos, side);
        boolean crystalOpen = opensCrystalPlacement(pos, target);
        boolean lowY = pos.getY() <= MathHelper.floor(target.posY);
        boolean selfDanger = isSelfTrapRisk(pos);

        switch (selectionMode.getValue())
        {
            case SPEED:
                score += mineSpeed * 200.0;
                score -= playerDistance * 2.0;
                reason = "Speed";
                break;

            case DISTANCE:
                score -= playerDistance * 10.0;
                score -= targetDistance * 2.0;
                reason = "Distance";
                break;

            case CRYSTAL:
                score += crystalOpen ? 80.0 : 0.0;
                score += touchesTargetBox(target, pos) ? 30.0 : 0.0;
                score += mineSpeed * 80.0;
                reason = "Crystal";
                break;

            case SMART:
            default:
                score += touchesTargetBox(target, pos) ? 45.0 : 0.0;
                score += crystalOpen && preferCrystalOpen.getValue() ? 60.0 : 0.0;
                score += preferFastMine.getValue() ? mineSpeed * 140.0 : mineSpeed * 60.0;
                score += visible && preferVisible.getValue() ? 20.0 : 0.0;
                score += lowY && preferLowY.getValue() ? 10.0 : 0.0;
                score -= playerDistance * 2.5;
                score -= targetDistance * 1.5;

                if (selfDanger && avoidSelfTrap.getValue())
                {
                    score -= 100.0;
                }

                reason = crystalOpen ? "City+" : "City";
                break;
        }

        if (!visible)
        {
            score -= 15.0;
        }

        if (mineSpeed <= 0.01f)
        {
            score -= 50.0;
        }

        if (CombatCoordinator.isFeetTarget(pos))
        {
            score -= 80.0;
        }

        if (CombatCoordinator.getCrystalPlaceTargets().contains(pos))
        {
            score -= 25.0;
        }

        return new CityCandidate(pos, side, score, reason);
    }

    protected boolean touchesTargetBox(EntityPlayer target, BlockPos pos)
    {
        AxisAlignedBB targetBB = target.getEntityBoundingBox().grow(0.01);
        AxisAlignedBB blockBB = new AxisAlignedBB(pos);

        BlockPos feet = new BlockPos(
            MathHelper.floor(target.posX),
            MathHelper.floor(target.posY),
            MathHelper.floor(target.posZ)
        );

        return targetBB.intersects(blockBB)
            || targetBB.intersects(blockBB.offset(0, -1, 0))
            || pos.distanceSq(feet) <= 2.0;
    }

    protected boolean opensCrystalPlacement(BlockPos minedBlock, EntityPlayer target)
    {
        BlockPos[] bases =
        {
            minedBlock.down(),
            minedBlock.down().north(),
            minedBlock.down().south(),
            minedBlock.down().east(),
            minedBlock.down().west(),
            minedBlock.north().down(),
            minedBlock.south().down(),
            minedBlock.east().down(),
            minedBlock.west().down()
        };

        for (BlockPos base : bases)
        {
            if (canPlaceCrystalAfterMine(base, minedBlock))
            {
                double dist = target.getDistance(
                    base.getX() + 0.5,
                    base.getY() + 1.0,
                    base.getZ() + 0.5
                );

                if (dist <= 4.5)
                {
                    return true;
                }
            }
        }

        return false;
    }

    protected boolean canPlaceCrystalAfterMine(BlockPos base, BlockPos minedBlock)
    {
        IBlockState baseState = mc.world.getBlockState(base);
        Block block = baseState.getBlock();

        if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK)
        {
            return false;
        }

        BlockPos up = base.up();
        BlockPos up2 = base.up(2);

        boolean upAir = mc.world.isAirBlock(up) || up.equals(minedBlock);
        boolean up2Air = mc.world.isAirBlock(up2) || up2.equals(minedBlock);

        return upAir && up2Air;
    }

    protected boolean canSeeMineSide(BlockPos pos, EnumFacing facing)
    {
        Vec3d eyes = mc.player.getPositionEyes(1.0f);

        Vec3d hitVec = new Vec3d(pos)
            .add(0.5, 0.5, 0.5)
            .add(new Vec3d(facing.getDirectionVec()).scale(0.5));

        RayTraceResult result = mc.world.rayTraceBlocks(
            eyes,
            hitVec,
            false,
            true,
            false
        );

        return result == null
            || result.typeOfHit == RayTraceResult.Type.MISS
            || result.getBlockPos().equals(pos);
    }

    protected boolean isSelfTrapRisk(BlockPos pos)
    {
        BlockPos self = new BlockPos(
            MathHelper.floor(mc.player.posX),
            MathHelper.floor(mc.player.posY),
            MathHelper.floor(mc.player.posZ)
        );

        return pos.equals(self)
            || pos.equals(self.north())
            || pos.equals(self.south())
            || pos.equals(self.east())
            || pos.equals(self.west())
            || pos.equals(self.up());
    }

    protected float estimateBestDamage(BlockPos pos, IBlockState state)
    {
        float best = 0.0f;

        for (int i = 0; i < 9; i++)
        {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            float damage = estimateMineDamage(pos, state, stack);

            if (damage > best)
            {
                best = damage;
            }
        }

        return best;
    }

    protected float estimateMineDamage(BlockPos pos, IBlockState state, ItemStack stack)
    {
        float hardness = state.getBlockHardness(mc.world, pos);

        if (hardness <= 0.0f)
        {
            return 0.0f;
        }

        float speed = stack == null || stack.isEmpty()
            ? 1.0f
            : stack.getDestroySpeed(state);

        if (speed > 1.0f)
        {
            int efficiency =
                EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, stack);

            if (efficiency > 0)
            {
                speed += efficiency * efficiency + 1;
            }
        }

        Potion haste = Potion.getPotionFromResourceLocation("haste");
        Potion fatigue = Potion.getPotionFromResourceLocation("mining_fatigue");

        PotionEffect hasteEffect =
            haste == null ? null : mc.player.getActivePotionEffect(haste);

        PotionEffect fatigueEffect =
            fatigue == null ? null : mc.player.getActivePotionEffect(fatigue);

        if (hasteEffect != null)
        {
            speed *= 1.0f + (hasteEffect.getAmplifier() + 1) * 0.2f;
        }

        if (fatigueEffect != null)
        {
            float fatigueMul;

            switch (fatigueEffect.getAmplifier())
            {
                case 0:
                    fatigueMul = 0.3f;
                    break;
                case 1:
                    fatigueMul = 0.09f;
                    break;
                case 2:
                    fatigueMul = 0.0027f;
                    break;
                default:
                    fatigueMul = 0.00081f;
                    break;
            }

            speed *= fatigueMul;
        }

        boolean correctTool = stack != null && !stack.isEmpty() && stack.canHarvestBlock(state);
        float divisor = correctTool ? 30.0f : 100.0f;

        return speed / hardness / divisor;
    }

    protected void sendStartPackets(BlockPos pos, EnumFacing facing, boolean stop)
    {
        switch (mode.getValue())
        {
            case DOUBLE:
                if (builders.getValue())
                {
                    sendDig(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, facing);
                    sendDig(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, pos, facing);
                    sendDig(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, facing);
                    sendDig(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, facing);
                    sendDig(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, pos, facing);

                    if (swing.getValue())
                    {
                        sendSwing(EnumHand.MAIN_HAND);
                        sendSwing(EnumHand.MAIN_HAND);
                        sendSwing(EnumHand.MAIN_HAND);
                    }
                }
                else
                {
                    sendDig(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, facing);
                    sendDig(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, facing);
                    sendDig(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, facing);
                    sendDig(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, facing);
                    sendDig(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, facing);
                    sendDig(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, facing);

                    if (swing.getValue())
                    {
                        sendSwing(EnumHand.MAIN_HAND);
                    }
                }

                break;

            case NORMAL:
                sendDig(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, facing);

                if (swing.getValue())
                {
                    doSwing(EnumHand.MAIN_HAND);
                }

                if (builders.getValue())
                {
                    if (swing.getValue())
                    {
                        doSwing(EnumHand.MAIN_HAND);
                    }

                    sendDig(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, pos, facing);
                }

                break;
        }
    }

    protected void sendStopAbort(BlockPos pos, EnumFacing facing)
    {
        if (swing.getValue())
        {
            doSwing(EnumHand.MAIN_HAND);
        }

        sendDig(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, facing);

        if (swing.getValue())
        {
            doSwing(EnumHand.MAIN_HAND);
        }

        sendDig(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, pos, facing);
    }

    public void startPacketMining(BlockPos pos, EnumFacing facing, boolean queued)
    {
        if (packetData == null)
        {
            packetData = new BreakData(pos, facing);
            packetData.start();
        }
        else
        {
            packetData.start(pos, facing);
        }

        packetData.queued = queued;

        if (rotate.getValue())
        {
            rotateTo(pos, facing);
        }

        sendStartPackets(pos, facing, true);
    }

    public void startNormalMining(BlockPos pos, EnumFacing facing, boolean queued)
    {
        didAction = true;

        if (normalData == null)
        {
            normalData = new BreakData(pos, facing);
            normalData.start();
        }
        else
        {
            normalData.start(pos, facing);
        }

        normalData.queued = queued;

        if (rotate.getValue())
        {
            rotateTo(pos, facing);
        }

        sendStartPackets(pos, facing, false);
    }

    public void abort(BreakData data)
    {
        if (data == null || data.pos == null || data.facing == null)
        {
            return;
        }

        sendDig(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, data.pos, data.facing);
        data.reset();
    }

    public boolean canMine(BlockPos pos)
    {
        if (pos == null)
        {
            return false;
        }

        if (isPacketMining(pos) || isMining(pos))
        {
            return false;
        }

        if (mainQueue != null && mainQueue.pos.equals(pos))
        {
            return false;
        }

        if (packetQueue != null && packetQueue.pos.equals(pos))
        {
            return false;
        }

        return isMineable(mc.world.getBlockState(pos), pos);
    }

    protected boolean canPacketMine()
    {
        return packetData == null || !packetData.isMining();
    }

    protected boolean isAnyMining(BlockPos pos)
    {
        return isMining(pos) || isPacketMining(pos);
    }

    public boolean isMining()
    {
        return normalData != null && normalData.isMining();
    }

    public boolean isPacketMining()
    {
        return packetData != null && packetData.isMining();
    }

    public boolean isMining(BlockPos pos)
    {
        return normalData != null && normalData.isMining(pos);
    }

    public boolean isPacketMining(BlockPos pos)
    {
        return packetData != null && packetData.isMining(pos);
    }

    protected boolean isBlockDelayGrim()
    {
        return System.currentTimeMillis() - lastBreak < 50L;
    }

    protected boolean shouldPauseForCombat()
    {
        return pauseCombatPlace.getValue()
            && (CombatCoordinator.isAutoCrystalPlacing()
                || CombatCoordinator.isFeetTrapPlacing());
    }

    protected void handleSwitchBack()
    {
        if (!switchBack)
        {
            return;
        }

        if (longSwitchBack)
        {
            longSwitchBack = false;
            return;
        }

        switchBack = false;

        if (swapMode.getValue() == SwapMode.SILENT
            || swapMode.getValue() == SwapMode.SIX_B6T
            || swapMode.getValue() == SwapMode.ALTER)
        {
            switchTo(switchBackSlot);
        }
    }

    protected EntityPlayer findTarget()
    {
        EntityPlayer best = null;
        double bestDistance = Double.MAX_VALUE;

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

            double distance = mc.player.getDistanceSq(player);

            if (distance < bestDistance)
            {
                bestDistance = distance;
                best = player;
            }
        }

        return best;
    }

    protected EnumFacing getMineableSide(BlockPos pos)
    {
        EnumFacing[] order =
        {
            EnumFacing.UP,
            EnumFacing.NORTH,
            EnumFacing.SOUTH,
            EnumFacing.EAST,
            EnumFacing.WEST,
            EnumFacing.DOWN
        };

        for (EnumFacing facing : order)
        {
            if (strictDirection.getValue() && !canStrictMine(pos, facing))
            {
                continue;
            }

            if (inRange(pos, facing))
            {
                return facing;
            }
        }

        if (strictDirection.getValue() && strictFallback.getValue())
        {
            for (EnumFacing facing : order)
            {
                if (inRange(pos, facing))
                {
                    return facing;
                }
            }
        }

        return null;
    }

    protected boolean canStrictMine(BlockPos pos, EnumFacing facing)
    {
        Vec3d eyes = mc.player.getPositionEyes(1.0f);
        Vec3d hitVec = new Vec3d(pos)
            .add(0.5, 0.5, 0.5)
            .add(new Vec3d(facing.getDirectionVec()).scale(0.5));

        RayTraceResult result = mc.world.rayTraceBlocks(
            eyes,
            hitVec,
            false,
            true,
            false
        );

        if (result != null
            && result.typeOfHit == RayTraceResult.Type.BLOCK
            && result.getBlockPos().equals(pos)
            && result.sideHit == facing)
        {
            return true;
        }

        if (airStrict.getValue())
        {
            return false;
        }

        return eyes.squareDistanceTo(hitVec) <= square(wallsRange.getValue());
    }

    protected boolean inRange(BlockPos pos, EnumFacing facing)
    {
        Vec3d hitVec = new Vec3d(pos)
            .add(0.5, 0.5, 0.5)
            .add(new Vec3d(facing.getDirectionVec()).scale(0.5));

        double distance = mc.player.getPositionEyes(1.0f).distanceTo(hitVec);

        if (distance > breakRange.getValue())
        {
            return false;
        }

        RayTraceResult result = mc.world.rayTraceBlocks(
            mc.player.getPositionEyes(1.0f),
            hitVec,
            false,
            true,
            false
        );

        if (result != null
            && result.typeOfHit == RayTraceResult.Type.BLOCK
            && !result.getBlockPos().equals(pos))
        {
            return distance <= wallsRange.getValue();
        }

        return true;
    }

    protected void rotateTo(BlockPos pos, EnumFacing facing)
    {
        Vec3d hitVec = new Vec3d(pos)
            .add(0.5, 0.5, 0.5)
            .add(new Vec3d(facing.getDirectionVec()).scale(0.5));

        double x = hitVec.x - mc.player.posX;
        double y = hitVec.y - (mc.player.posY + mc.player.getEyeHeight());
        double z = hitVec.z - mc.player.posZ;
        double dist = Math.sqrt(x * x + z * z);

        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(y, dist));

        Managers.ROTATION.setBlocking(true);
        mc.player.connection.sendPacket(new CPacketPlayer.Rotation(yaw, pitch, mc.player.onGround));
        Managers.ROTATION.setBlocking(false);
    }

    protected void switchTo(int slot)
    {
        if (slot < 0 || slot > 8 || mc.player.inventory.currentItem == slot)
        {
            return;
        }

        mc.player.connection.sendPacket(new CPacketHeldItemChange(slot));
        mc.player.inventory.currentItem = slot;
        InventoryUtil.syncItem();
    }

    protected void sendDig(CPacketPlayerDigging.Action action, BlockPos pos, EnumFacing facing)
    {
        mc.player.connection.sendPacket(new CPacketPlayerDigging(action, pos, facing));
    }

    protected void doSwing(EnumHand hand)
    {
        if (!swing.getValue())
        {
            return;
        }

        if (packetSwing.getValue())
        {
            sendSwing(hand);
        }
        else
        {
            mc.player.swingArm(hand);
        }
    }

    protected void sendSwing(EnumHand hand)
    {
        mc.player.connection.sendPacket(new CPacketAnimation(hand));
    }

    protected boolean isMineable(IBlockState state, BlockPos pos)
    {
        Block block = state.getBlock();

        return !(block instanceof BlockAir)
            && !(block instanceof BlockLiquid)
            && state.getBlockHardness(mc.world, pos) >= 0.0f;
    }

    protected boolean isAir(BlockPos pos)
    {
        IBlockState state = mc.world.getBlockState(pos);

        return state.getBlock() instanceof BlockAir
            || state.getBlock() instanceof BlockLiquid;
    }

    protected double distanceTo(BlockPos pos)
    {
        return mc.player.getDistance(pos.getX() + 0.5,
                                     pos.getY() + 0.5,
                                     pos.getZ() + 0.5);
    }

    protected double square(double value)
    {
        return value * value;
    }

    protected boolean passed(long last, long delay)
    {
        return System.currentTimeMillis() - last >= delay;
    }

    protected void renderData(BreakData data, boolean packet)
    {
        if (data == null || !data.isMining() || data.pos == null || data.isAir())
        {
            return;
        }

        float max = packet ? 1.0f : breakAt.getValue();
        float progress = MathHelper.clamp(data.bestDamage / Math.max(0.01f, max), 0.0f, 1.0f);

        Color fill = packet ? fillColor2.getValue() : fillColor.getValue();
        Color line = packet ? lineColor2.getValue() : lineColor.getValue();

        int fillAlpha = MathHelper.clamp((int) (fill.getAlpha() * progress), 0, fill.getAlpha());
        int lineAlpha = MathHelper.clamp((int) (line.getAlpha() * progress), 0, line.getAlpha());

        RenderUtil.renderBox(
            Interpolation.interpolatePos(data.pos, 1.0f),
            new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), fillAlpha),
            new Color(line.getRed(), line.getGreen(), line.getBlue(), lineAlpha),
            1.5f
        );
    }

    protected void onRender(Render3DEvent event)
    {
        if (!render.getValue() || mc.player == null || mc.world == null)
        {
            return;
        }

        if (normalData != null && normalData.isMining() && !normalData.isAir())
        {
            renderData(normalData, false);
        }

        if (mode.getValue() == BreakMode.DOUBLE
            && packetData != null
            && packetData.isMining()
            && !packetData.isAir())
        {
            renderData(packetData, true);
        }
    }

    public final class BreakData
    {
        private BlockPos pos;
        private EnumFacing facing;
        private final float[] damages = new float[9];

        private float bestDamage;
        private float lastBestDamage;
        private int bestSlot = -1;
        private long startTime;
        private long timeoutStart;
        private boolean queued;
        private IBlockState lastNonAirState;
        private boolean mining;
        private boolean lastAir;
        private int attempts;
        private boolean resetOverride;

        public BreakData(BlockPos pos, EnumFacing facing)
        {
            this.pos = pos;
            this.facing = facing;
            this.startTime = System.currentTimeMillis();
            this.timeoutStart = System.currentTimeMillis();
            this.lastNonAirState = mc.world == null ? null : mc.world.getBlockState(pos);
        }

        public void start()
        {
            this.mining = true;
            this.startTime = System.currentTimeMillis();
            this.timeoutStart = System.currentTimeMillis();
        }

        public void start(BlockPos pos, EnumFacing facing)
        {
            this.pos = pos;
            this.facing = facing;
            reset();
            this.mining = true;
            this.queued = false;
            this.lastNonAirState = mc.world.getBlockState(pos);
            this.startTime = System.currentTimeMillis();
            this.timeoutStart = System.currentTimeMillis();
        }

        public void tick()
        {
            boolean curAir = isAir();

            if (!lastAir && curAir)
            {
                timeoutStart = System.currentTimeMillis();

                if (!onSuccess(this))
                {
                    return;
                }
            }

            if (!curAir)
            {
                lastNonAirState = mc.world.getBlockState(pos);
            }

            lastAir = curAir;
            updateDamage();

            if (isReady(false)
                && rebreak.getValue() == RebreakMode.FAST
                && curAir)
            {
                abort(this);
            }
        }

        public float updateDamage()
        {
            lastBestDamage = bestDamage;

            IBlockState state = lastNonAirState == null
                ? mc.world.getBlockState(pos)
                : lastNonAirState;

            for (int i = 0; i < 9; i++)
            {
                ItemStack stack = mc.player.inventory.getStackInSlot(i);
                float damage = getMineDamage(state, stack);
                damages[i] = MathHelper.clamp(damages[i] + damage, 0.0f, Float.MAX_VALUE);

                if (damages[i] > bestDamage)
                {
                    bestDamage = damages[i];
                    bestSlot = i;
                }
            }

            return bestDamage;
        }

        public float getMineDamage(IBlockState state, ItemStack stack)
        {
            return estimateMineDamage(pos, state, stack);
        }

        public void updateBlock(IBlockState state)
        {
            boolean air = state.getBlock() instanceof BlockAir
                || state.getBlock() instanceof BlockLiquid;

            if (!lastAir && air)
            {
                timeoutStart = System.currentTimeMillis();

                if (!onSuccess(this))
                {
                    return;
                }
            }

            if (!air)
            {
                lastNonAirState = state;
            }

            lastAir = air;

            if (isReady(false)
                && rebreak.getValue() == RebreakMode.FAST
                && air)
            {
                abort(this);
            }
        }

        public boolean timeout()
        {
            if (attempts < 1)
            {
                timeoutStart = System.currentTimeMillis();
                return System.currentTimeMillis() - startTime > timeout.getValue();
            }

            return System.currentTimeMillis() - timeoutStart > mineTimeout.getValue();
        }

        public boolean isReady(boolean packet)
        {
            return bestDamage >= (packet ? 1.0f : breakAt.getValue());
        }

        public boolean isMining()
        {
            return mining;
        }

        public boolean isMining(BlockPos block)
        {
            return mining && pos != null && pos.equals(block);
        }

        public boolean isAir()
        {
            return FastBreak.this.isAir(pos);
        }

        public BlockPos getPos()
        {
            return pos;
        }

        public EnumFacing getFacing()
        {
            return facing;
        }

        public int getBestSlot()
        {
            return bestSlot;
        }

        public void markAttempt()
        {
            if (attempts < 1)
            {
                timeoutStart = System.currentTimeMillis();
                startTime = System.currentTimeMillis();
            }

            attempts++;
        }

        public void reset()
        {
            for (int i = 0; i < damages.length; i++)
            {
                damages[i] = 0.0f;
            }

            bestDamage = 0.0f;
            lastBestDamage = 0.0f;
            bestSlot = -1;
            attempts = 0;
            mining = false;
            lastAir = false;
            resetOverride = false;
            queued = false;
            startTime = System.currentTimeMillis();
            timeoutStart = System.currentTimeMillis();
        }
    }

    public boolean onSuccess(BreakData data)
    {
        if (data == normalData)
        {
            if (rebreak.getValue() == RebreakMode.FAST)
            {
                if (data.attempts < 1)
                {
                    return true;
                }
            }

            if (rebreak.getValue() != RebreakMode.INSTA || data.resetOverride)
            {
                data.reset();
                return false;
            }
        }
        else if (data == packetData)
        {
            data.reset();
            return false;
        }

        return true;
    }

    private static final class BreakRequest
    {
        private final BlockPos pos;
        private final EnumFacing facing;
        private final boolean reset;

        private BreakRequest(BlockPos pos, EnumFacing facing, boolean reset)
        {
            this.pos = pos;
            this.facing = facing;
            this.reset = reset;
        }
    }

    private static final class CityCandidate
    {
        private final BlockPos pos;
        private final EnumFacing facing;
        private final double score;
        private final String reason;

        private CityCandidate(BlockPos pos,
                              EnumFacing facing,
                              double score,
                              String reason)
        {
            this.pos = pos;
            this.facing = facing;
            this.score = score;
            this.reason = reason;
        }
    }

    public enum Page
    {
        BREAKING,
        AUTOCITY,
        RENDER
    }

    public enum BreakMode
    {
        NORMAL,
        DOUBLE
    }

    public enum DoubleLogic
    {
        ALWAYS,
        ONLY,
        BASIC
    }

    public enum SwapMode
    {
        NONE,
        SILENT,
        ALTER,
        SIX_B6T
    }

    public enum RebreakMode
    {
        NONE,
        NORMAL,
        INSTA,
        FAST
    }

    public enum PauseMode
    {
        NONE,
        EAT
    }

    public enum SelectionMode
    {
        SMART,
        SPEED,
        DISTANCE,
        CRYSTAL
    }

    private static final class ListenerTick
        extends ModuleListener<FastBreak, TickEvent>
    {
        private ListenerTick(FastBreak module)
        {
            super(module, TickEvent.class);
        }

        @Override
        public void invoke(TickEvent event)
        {
            if (event.isSafe())
            {
                module.onTick();
            }
        }
    }

    private static final class ListenerPostTick
        extends ModuleListener<FastBreak, TickEvent.Post>
    {
        private ListenerPostTick(FastBreak module)
        {
            super(module, TickEvent.Post.class);
        }

        @Override
        public void invoke(TickEvent.Post event)
        {
            if (event.isSafe())
            {
                module.onPostTick();
            }
        }
    }

    private static final class ListenerClickBlock
        extends ModuleListener<FastBreak, ClickBlockEvent>
    {
        private ListenerClickBlock(FastBreak module)
        {
            super(module, ClickBlockEvent.class);
        }

        @Override
        public void invoke(ClickBlockEvent event)
        {
            module.onClickBlock(event);
        }
    }

    private static final class ListenerDamageBlock
        extends ModuleListener<FastBreak, DamageBlockEvent>
    {
        private ListenerDamageBlock(FastBreak module)
        {
            super(module, DamageBlockEvent.class);
        }

        @Override
        public void invoke(DamageBlockEvent event)
        {
            module.onDamageBlock(event);
        }
    }

    private static final class ListenerPacketSend
        extends ModuleListener<FastBreak, PacketEvent.Send<?>>
    {
        private ListenerPacketSend(FastBreak module)
        {
            super(module, PacketEvent.Send.class);
        }

        @Override
        public void invoke(PacketEvent.Send<?> event)
        {
            module.onPacketSend(event);
        }
    }

    private static final class ListenerBlockChange
        extends ModuleListener<FastBreak, PacketEvent.Receive<SPacketBlockChange>>
    {
        private ListenerBlockChange(FastBreak module)
        {
            super(module, PacketEvent.Receive.class, SPacketBlockChange.class);
        }

        @Override
        public void invoke(PacketEvent.Receive<SPacketBlockChange> event)
        {
            module.onBlockChange(event.getPacket().getBlockPosition(),
                                 event.getPacket().getBlockState());
        }
    }

    private static final class ListenerMultiBlockChange
        extends ModuleListener<FastBreak, PacketEvent.Receive<SPacketMultiBlockChange>>
    {
        private ListenerMultiBlockChange(FastBreak module)
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

    private static final class ListenerRender
        extends ModuleListener<FastBreak, Render3DEvent>
    {
        private ListenerRender(FastBreak module)
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

