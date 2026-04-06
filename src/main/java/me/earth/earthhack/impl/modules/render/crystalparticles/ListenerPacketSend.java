package me.earth.earthhack.impl.modules.render.crystalparticles;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.network.play.client.CPacketUseEntity;

final class ListenerPacketSend
        extends ModuleListener<Crystalparticles, PacketEvent.Send<?>>
{
    public ListenerPacketSend(Crystalparticles module)
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

        Entity entity = packet.getEntityFromWorld(mc.world);
        if (!(entity instanceof EntityEnderCrystal))
        {
            return;
        }

        if (module.critParticles.getValue())
        {
            mc.player.onCriticalHit(entity);
        }

        if (module.magicParticles.getValue())
        {
            mc.player.onEnchantmentCritical(entity);
        }
    }
}
