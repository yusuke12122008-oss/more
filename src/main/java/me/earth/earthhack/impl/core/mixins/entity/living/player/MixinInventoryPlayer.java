package me.earth.earthhack.impl.core.mixins.entity.living.player;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.modules.Caches;

import me.earth.earthhack.impl.util.thread.Locks;
import net.minecraft.entity.player.InventoryPlayer;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryPlayer.class)
public abstract class MixinInventoryPlayer
{


    @Redirect(
        method = "setPickedItemStack",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/entity/player/InventoryPlayer;currentItem:I",
            opcode = Opcodes.PUTFIELD))
    public void setPickedItemStackHook(InventoryPlayer inventoryPlayer,
                                        int value)
    {
        Locks.acquire(Locks.PLACE_SWITCH_LOCK,
                () -> inventoryPlayer.currentItem = value);
    }

    @Redirect(
        method = "pickItem",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/entity/player/InventoryPlayer;currentItem:I",
            opcode = Opcodes.PUTFIELD))
    public void pickItemHook(InventoryPlayer inventoryPlayer,
                              int value)
    {
        Locks.acquire(Locks.PLACE_SWITCH_LOCK,
                () -> inventoryPlayer.currentItem = value);
    }



}
