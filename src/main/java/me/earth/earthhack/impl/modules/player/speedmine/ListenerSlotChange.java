package me.earth.earthhack.impl.modules.player.speedmine;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.network.play.client.CPacketHeldItemChange;

final class ListenerSlotChange
    extends ModuleListener<Speedmine, PacketEvent.Send<CPacketHeldItemChange>>
{
    ListenerSlotChange(Speedmine module)
    {
        super(module, PacketEvent.Send.class, CPacketHeldItemChange.class);
    }

    @Override
    public void invoke(PacketEvent.Send<CPacketHeldItemChange> event)
    {
        if (module.pos == null)
        {
            module.lastSelectedSlot = event.getPacket().getSlotId();
            return;
        }

        if (module.selfSwitching || module.ignoreSlotResetTicks > 0)
        {
            module.lastSelectedSlot = event.getPacket().getSlotId();
            return;
        }

        int newSlot = event.getPacket().getSlotId();
        if (newSlot != module.lastSelectedSlot)
        {
            module.onManualSlotChange(newSlot);
        }
    }
}
