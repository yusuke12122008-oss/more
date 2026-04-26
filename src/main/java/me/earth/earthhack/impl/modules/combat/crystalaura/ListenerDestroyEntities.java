package me.earth.earthhack.impl.modules.combat.crystalaura;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.network.play.server.SPacketDestroyEntities;

final class ListenerDestroyEntities extends ModuleListener<CrystalAura, PacketEvent.Receive<SPacketDestroyEntities>>
{
    ListenerDestroyEntities(CrystalAura module)
    {
        super(module, PacketEvent.Receive.class, SPacketDestroyEntities.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketDestroyEntities> event)
    {
        if (mc.player == null || mc.world == null)
        {
            return;
        }

        synchronized (module.stateLock)
        {
            for (int id : event.getPacket().getEntityIDs())
            {
                Entity e = mc.world.getEntityByID(id);
                if (e instanceof EntityEnderCrystal)
                {
                    module.positionCache.markExploded(e.getPosition());
                    if (module.endSequenceOnBreak.getValue())
                    {
                        module.clearSequence();
                    }
                }
            }
        }
    }
}
