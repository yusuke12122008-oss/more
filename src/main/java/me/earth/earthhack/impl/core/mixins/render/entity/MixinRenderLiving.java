package me.earth.earthhack.impl.core.mixins.render.entity;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.modules.Caches;


import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderLiving.class)
public abstract class MixinRenderLiving
{


    @Inject(
        method = "renderLeash",
        at = @At("HEAD"),
        cancellable = true)
    public void renderLeashHook(CallbackInfo info)
    {

    }



}
