package me.earth.earthhack.impl.core.mixins.render.entity;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.modules.Caches;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPlayer.class)
public abstract class MixinRenderPlayer
{


    @Inject(
            method = "renderEntityName",
            at = @At("HEAD"),
            cancellable = true)
    public void renderEntityNameHook(AbstractClientPlayer entityIn,
                                     double x,
                                     double y,
                                     double z,
                                     String name,
                                     double distanceSq,
                                     CallbackInfo info)
    {
    }

}
