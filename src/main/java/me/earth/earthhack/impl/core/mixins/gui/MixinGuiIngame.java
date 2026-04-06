package me.earth.earthhack.impl.core.mixins.gui;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.api.cache.SettingCache;
import me.earth.earthhack.api.event.bus.instance.Bus;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.api.setting.settings.EnumSetting;
import me.earth.earthhack.impl.event.events.render.CrosshairEvent;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.client.hud.HUD;
import me.earth.earthhack.impl.modules.client.hud.modes.Potions;

import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngame.class)
public abstract class MixinGuiIngame
{




    @Inject(
        method = "renderPotionEffects",
        at = @At("HEAD"),
        cancellable = true)
    protected void renderPotionEffectsHook(ScaledResolution scaledRes,
                                           CallbackInfo info)
    {
        if (POTS.getValue() == Potions.Hide || POTS.getValue() == Potions.Text)
        {
            info.cancel();
        }
    }

    @Inject(
            method = "renderAttackIndicator",
            at = @At("HEAD"),
            cancellable = true)
    protected void renderAttackIndicator(float partialTicks, ScaledResolution p_184045_2_, CallbackInfo ci)
    {
        final CrosshairEvent event = new CrosshairEvent();
        Bus.EVENT_BUS.post(event);
        if (event.isCancelled())
            ci.cancel();
    }



}
