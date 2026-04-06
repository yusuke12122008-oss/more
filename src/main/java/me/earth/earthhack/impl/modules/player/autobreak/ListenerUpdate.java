package me.earth.earthhack.impl.modules.player.autobreak;

import me.earth.earthhack.api.event.events.Stage;
import me.earth.earthhack.impl.event.events.network.MotionUpdateEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.math.rotation.RotationUtil;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

public class ListenerUpdate extends ModuleListener<AutoBreak, MotionUpdateEvent> {
    public ListenerUpdate(AutoBreak module) {
        super(module, MotionUpdateEvent.class);
    }

    @Override
    public void invoke(MotionUpdateEvent event) {
        if (module.ignoreSlotResetTicks > 0) {
            module.ignoreSlotResetTicks--;
        }

        if (event.getStage() == Stage.PRE) {
            if (module.pauseEverything) {
                module.pauseEverything = false;
            }

            if (module.stopPos != null) {
                if (mc.world.getBlockState(module.stopPos).getBlock() == Blocks.AIR) {
                    if (module.remine.getValue() != AutoBreak.RemineMode.None) {
                        module.lastConfirmedBroke = module.stopPos;
                    }
                    module.stopPos = null;
                } else if (module.stopTimer.passed(350L)) {
                    module.mining = module.stopPos;
                    module.stopPos = null;
                    module.resetProgress();
                    module.restartCurrentTarget();
                }
            }

            if (module.mining == null
                && module.lastConfirmedBroke != null
                && module.remine.getValue() != AutoBreak.RemineMode.None
                && mc.world.getBlockState(module.lastConfirmedBroke).getBlock() != Blocks.AIR) {
                module.mining = module.lastConfirmedBroke;
                if (module.facing == null) {
                    module.facing = EnumFacing.UP;
                }
                module.resetProgress();
                module.restartCurrentTarget();
                if (module.remine.getValue() == AutoBreak.RemineMode.Instant) {
                    for (int i = 0; i < module.damages.length; i++) {
                        module.damages[i] = 1.0f;
                    }
                    module.maxDamage = 1.0f;
                }
                module.lastConfirmedBroke = null;
            }

            if (module.mining == null || module.getPickSlot() == -1) {
                module.lastSlot = mc.player.inventory.currentItem;
                return;
            }

            if (mc.world.getBlockState(module.mining).getBlock() == Blocks.AIR) {
                if (module.remine.getValue() != AutoBreak.RemineMode.None) {
                    module.lastConfirmedBroke = module.mining;
                }
                module.clearTarget();
                module.lastSlot = mc.player.inventory.currentItem;
                return;
            }

            module.maxDamage = 0.0f;
            int slot = module.updateDamage();

            if (module.maxDamage >= 1.0f) {
                if (!module.delay) {
                    module.delay = true;
                } else {
                    module.delay = false;

                    if (mc.player.getDistance(module.mining.getX() + 0.5, module.mining.getY() + 0.5, module.mining.getZ() + 0.5) > 6.0) {
                        module.clearTarget();
                        module.lastSlot = mc.player.inventory.currentItem;
                        return;
                    }

                    if (module.facing == null) {
                        module.facing = EnumFacing.UP;
                    }

                    if (module.rotate.getValue() && module.facing != null) {
                        float[] rotations = RotationUtil.getRotations(module.mining.offset(module.facing), module.facing.getOpposite());
                        if (rotations != null) {
                            event.setYaw(rotations[0]);
                            event.setPitch(rotations[1]);
                        }
                    }

                    int old = mc.player.inventory.currentItem;
                    boolean doSwitch = slot != old;
                    if (doSwitch) {
                        ItemStack newStack = mc.player.inventory.getStackInSlot(slot);
                        ItemStack oldStack = mc.player.inventory.getStackInSlot(old);
                        if (newStack == oldStack) {
                            doSwitch = false;
                        }
                    }

                    module.shouldSendStop = true;
                    module.stopTargetSlot = doSwitch ? slot : -1;
                }
            }

            if (mc.player.onGround) {
                module.groundedTicks++;
            } else {
                module.airTicks++;
            }

            module.lastSlot = mc.player.inventory.currentItem;
            return;
        }

        if (!module.shouldSendStop || module.mining == null || module.facing == null) {
            return;
        }

        BlockPos targetPos = module.mining;
        EnumFacing targetFacing = module.facing;
        int slot = module.stopTargetSlot;

        module.shouldSendStop = false;

        int old = module.beginSilentToolSwap(slot);

        mc.player.swingArm(EnumHand.MAIN_HAND);
        module.pauseEverything = true;
        mc.player.connection.sendPacket(new CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, targetPos, targetFacing));

        module.endSilentToolSwap(old);

        module.stopPos = targetPos;
        module.stopTimer.reset();
        module.mining = null;
        module.resetProgress();
        module.cooldown.reset();
    }
}
