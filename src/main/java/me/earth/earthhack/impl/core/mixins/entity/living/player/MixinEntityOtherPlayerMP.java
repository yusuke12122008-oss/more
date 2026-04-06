package me.earth.earthhack.impl.core.mixins.entity.living.player;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.core.ducks.entity.IEntityOtherPlayerMP;
import me.earth.earthhack.impl.modules.Caches;

import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.util.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityOtherPlayerMP.class)
public abstract class MixinEntityOtherPlayerMP extends MixinAbstractClientPlayer
        implements IEntityOtherPlayerMP
{


    @Override
    public boolean attackEntitySuper(DamageSource source, float amount)
    {
        return super.attackEntityFrom(source, amount);
    }

    @Inject(method = "attackEntityFrom", at = @At("HEAD"), cancellable = true)
    public void attackEntityFromHook(DamageSource source,
                                      float amount,
                                      CallbackInfoReturnable<Boolean> cir)
    {
        if (this.shouldAttackSuper())
        {
            cir.setReturnValue(this.returnFromSuperAttack(source, amount));
        }
    }



}
