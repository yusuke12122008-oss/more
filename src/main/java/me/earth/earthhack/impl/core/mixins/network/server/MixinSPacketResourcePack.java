package me.earth.earthhack.impl.core.mixins.network.server;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.modules.Caches;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketResourcePackSend;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SPacketResourcePackSend.class)
public abstract class MixinSPacketResourcePack
{


    @Inject(method = "readPacketData", at = @At("HEAD"), cancellable = true)
    public void readPacketDataHook(PacketBuffer buf, CallbackInfo ci)
    {

    }

}
