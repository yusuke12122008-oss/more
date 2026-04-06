package me.earth.earthhack.impl.core.mixins.item;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.modules.Caches;

import net.minecraft.item.ItemExpBottle;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemExpBottle.class)
public abstract class MixinItemExpBottle
{


    @Redirect(
        method = "onItemRightClick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;shrink(I)V"))
    public void onItemRightClickHook(ItemStack stack, int quantity)
    {
        stack.shrink(quantity);
    }

}
