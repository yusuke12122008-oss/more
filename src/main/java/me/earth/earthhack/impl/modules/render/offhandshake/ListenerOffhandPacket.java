package me.earth.earthhack.impl.modules.render.offhandshake;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.network.play.client.CPacketUseEntity;

final class ListenerOffhandPacket
        extends ModuleListener<OffhandShake, PacketEvent.Send<?>>
{
    public ListenerOffhandPacket(OffhandShake module)
    {
        super(module, PacketEvent.Send.class);
    }

    @Override
    public void invoke(PacketEvent.Send<?> event)
    {
        if (mc.player == null || mc.world == null)
        {
            return;
        }

        if (!(event.getPacket() instanceof CPacketUseEntity))
        {
            return;
        }

        CPacketUseEntity packet = (CPacketUseEntity) event.getPacket();
        if (packet.getAction() != CPacketUseEntity.Action.ATTACK)
        {
            return;
        }

        Entity target = packet.getEntityFromWorld(mc.world);
        if (!(target instanceof EntityEnderCrystal))
        {
            return;
        }

        module.applyShake();
    }
}
