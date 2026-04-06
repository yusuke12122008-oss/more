package me.earth.earthhack.impl.core.mixins.render;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.api.event.bus.instance.Bus;
import me.earth.earthhack.impl.event.events.render.RenderArmorEvent;
import me.earth.earthhack.impl.modules.Caches;


import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

import static org.lwjgl.opengl.GL11.*;

@Mixin(LayerArmorBase.class)
public abstract class MixinLayerArmorBase {




    @Redirect(method = "renderArmorLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V"))
    public void renderArmorHook(ModelBase modelBase, Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        RenderArmorEvent pre = new RenderArmorEvent.Pre(entityIn, modelBase, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        Bus.EVENT_BUS.post(pre);
        if (!pre.isCancelled())
        {
            pre.getModel().render(entityIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }

        RenderArmorEvent post = new RenderArmorEvent.Post(entityIn, modelBase, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        Bus.EVENT_BUS.post(post);
    }



}
