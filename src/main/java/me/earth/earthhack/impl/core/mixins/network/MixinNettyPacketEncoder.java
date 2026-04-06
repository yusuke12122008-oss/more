package me.earth.earthhack.impl.core.mixins.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.Earthhack;
import me.earth.earthhack.impl.modules.Caches;

import net.minecraft.network.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(NettyPacketEncoder.class)
public abstract class MixinNettyPacketEncoder
{


    @Shadow
    @Final
    private EnumPacketDirection direction;

    @Inject(
        method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;Lio/netty/buffer/ByteBuf;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/Packet;writePacketData(Lnet/minecraft/network/PacketBuffer;)V",
            shift = At.Shift.AFTER))
    public void encodeHook(ChannelHandlerContext p_encode_1_,
                            Packet<?> p_encode_2_,
                            ByteBuf p_encode_3_,
                            CallbackInfo ci)
    {

    }

    @Redirect(method = "encode(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;Lio/netty/buffer/ByteBuf;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/EnumConnectionState;getPacketId(Lnet/minecraft/network/EnumPacketDirection;Lnet/minecraft/network/Packet;)Ljava/lang/Integer;"))
    private Integer hook(EnumConnectionState instance,
                         EnumPacketDirection direction, Packet<?> packetIn)
        throws Exception {
        Integer id = instance.getPacketId(direction, packetIn);
        //noinspection ConstantConditions
        if (id == null) {
            throw new IOException("Couldn't get Id for " + packetIn.getClass().getName());
        }

        return id;
    }

}
