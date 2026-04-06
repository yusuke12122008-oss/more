package me.earth.earthhack.impl.modules.player.autobreak;

import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.module.util.Category;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.BooleanSetting;
import me.earth.earthhack.api.setting.settings.ColorSetting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.impl.util.math.MathUtil;
import me.earth.earthhack.impl.util.math.StopWatch;
import me.earth.earthhack.impl.util.minecraft.InventoryUtil;
import me.earth.earthhack.impl.util.minecraft.blocks.mine.MineUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.awt.Color;

public class AutoBreak extends Module {
    public enum SwitchMode { Ghost, Alternative }
    public enum RemineMode { None, Normal, Instant }

    public final Setting<Color> fillColor = register(new ColorSetting("FillColor", new Color(255, 0, 0, 81)));
    public final Setting<Color> lineColor = register(new ColorSetting("OutlineColor", new Color(255, 0, 0, 255)));
    public final Setting<Color> fillColor2 = register(new ColorSetting("FillColor2", new Color(0, 255, 0, 81)));
    public final Setting<Color> lineColor2 = register(new ColorSetting("OutlineColor2", new Color(0, 255, 0, 255)));
    public final Setting<SwitchMode> swap = register(new EnumSetting<>("Switch", SwitchMode.Ghost));
    public final Setting<Boolean> strict = register(new BooleanSetting("Strict", true));
    public final Setting<Boolean> reset = register(new BooleanSetting("Reset", true));
    public final Setting<Boolean> rotate = register(new BooleanSetting("Rotate", true));
    public final Setting<RemineMode> remine = register(new EnumSetting<>("ReMine", RemineMode.None));
    public final Setting<Boolean> unbreak = register(new BooleanSetting("Unbreak", true));

    public final StopWatch cooldown = new StopWatch();
    public final StopWatch stopTimer = new StopWatch();
    public final StopWatch unbreakCooldown = new StopWatch();
    public final StopWatch unbreakSecondCooldown = new StopWatch();

    public final float[] damages = new float[9];

    public BlockPos mining;
    public BlockPos selectedPos;
    public EnumFacing facing;

    public BlockPos stopPos;
    public BlockPos lastConfirmedBroke;
    public BlockPos lastAttemptedBreak;

    public float maxDamage;
    public boolean delay;
    public boolean pauseEverything;
    public boolean shouldSendStop;
    public boolean scheduledRemine;
    public boolean beginCounting;
    public boolean selfSwitching;

    public int stopTargetSlot = -1;
    public int lastSlot = -1;
    public int ignoreSlotResetTicks;
    public int groundedTicks;
    public int airTicks;

    public AutoBreak() {
        super("AutoBreak", Category.Player);
        this.listeners.add(new ListenerUpdate(this));
        this.listeners.add(new ListenerRender(this));
        this.listeners.add(new ListenerDamage(this));
        this.listeners.add(new ListenerBlockClick(this));
        this.listeners.add(new ListenerPacketSend(this));
    }

    @Override
    protected void onDisable() {
        super.onDisable();
        clearTarget();
        stopPos = null;
        lastConfirmedBroke = null;
        lastAttemptedBreak = null;
        scheduledRemine = false;
        beginCounting = false;
        selfSwitching = false;
        ignoreSlotResetTicks = 0;
        stopTargetSlot = -1;
    }

    public void resetProgress() {
        delay = false;
        maxDamage = 0.0f;
        shouldSendStop = false;
        stopTargetSlot = -1;
        groundedTicks = 0;
        airTicks = 0;
        for (int i = 0; i < damages.length; i++) {
            damages[i] = 0.0f;
        }
    }

    public void clearTarget() {
        resetProgress();
        mining = null;
    }

    public void startMining(BlockPos pos, EnumFacing face) {
        mining = pos;
        selectedPos = pos;
        if (face != null) {
            facing = face;
        } else if (facing == null) {
            facing = EnumFacing.UP;
        }

        resetProgress();
        int oldSlot = beginSilentToolSwap(getBestToolSlot(pos));
        mc.player.connection.sendPacket(
            new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, mining, facing)
        );
        endSilentToolSwap(oldSlot);
    }

    public void restartCurrentTarget() {
        if (mining == null || facing == null) {
            return;
        }

        int oldSlot = beginSilentToolSwap(getBestToolSlot(mining));
        abortCurrentPos();
        resetProgress();
        mc.player.connection.sendPacket(
            new CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, mining, facing)
        );
        endSilentToolSwap(oldSlot);
    }


    public int getBestToolSlot(BlockPos pos) {
        int bestSlot = -1;
        float bestDamage = 0.0f;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            float damage = MineUtil.getDamage(stack, pos, true);
            if (damage > bestDamage) {
                bestDamage = damage;
                bestSlot = i;
            }
        }

        return bestSlot != -1 ? bestSlot : getPickSlot();
    }

    public int beginSilentToolSwap(int slot) {
        int oldSlot = mc.player.inventory.currentItem;
        if (slot < 0 || slot == oldSlot) {
            return -1;
        }

        selfSwitching = true;
        ignoreSlotResetTicks = Math.max(ignoreSlotResetTicks, 3);
        if (swap.getValue() == SwitchMode.Alternative) {
            mc.player.connection.sendPacket(new CPacketHeldItemChange(slot));
        } else {
            InventoryUtil.switchTo(slot);
        }

        return oldSlot;
    }

    public void endSilentToolSwap(int oldSlot) {
        if (oldSlot == -1) {
            selfSwitching = false;
            return;
        }

        ignoreSlotResetTicks = Math.max(ignoreSlotResetTicks, 3);
        if (swap.getValue() == SwitchMode.Alternative) {
            mc.player.connection.sendPacket(new CPacketHeldItemChange(oldSlot));
        } else {
            InventoryUtil.switchTo(oldSlot);
        }

        selfSwitching = false;
    }

    public void abortCurrentPos() {
        if (mining != null && facing != null) {
            mc.player.connection.sendPacket(
                new CPacketPlayerDigging(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, mining, facing)
            );
            mc.world.sendBlockBreakProgress(mc.player.getEntityId(), mining, -1);
            mc.player.resetCooldown();
        }
    }

    public int updateDamage() {
        int slot = -1;
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            float damage = MineUtil.getDamage(stack, mining, true);
            damages[i] = MathUtil.clamp(damages[i] + damage, 0.0f, Float.MAX_VALUE);
            if (damages[i] > maxDamage) {
                maxDamage = damages[i];
                slot = i;
            }
        }

        return slot;
    }

    int getPickSlot() {
        int pickSlot = -1;
        for (int i = 0; i < 9; ++i) {
            if (mc.player.inventory.getStackInSlot(i).getItem() instanceof net.minecraft.item.ItemPickaxe) {
                pickSlot = i;
                break;
            }
        }

        return pickSlot;
    }

    @Override
    public String getDisplayInfo() {
        return String.format("%.1f", maxDamage);
    }
}
