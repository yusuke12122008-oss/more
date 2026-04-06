package me.earth.earthhack.impl.modules.player.autobreak;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.network.play.client.CPacketHeldItemChange;

public class ListenerPacketSend extends ModuleListener<AutoBreak, PacketEvent.Send<?>> {
    public ListenerPacketSend(AutoBreak module) {
        super(module, PacketEvent.Send.class);
    }

    @Override
    public void invoke(PacketEvent.Send<?> event) {
        if (!(event.getPacket() instanceof CPacketHeldItemChange) || !module.reset.getValue()) {
            return;
        }

        if (module.selfSwitching || module.ignoreSlotResetTicks > 0 || module.mining == null) {
            return;
        }

        module.resetProgress();
        module.restartCurrentTarget();
        if (module.remine.getValue() != AutoBreak.RemineMode.None) {
            module.scheduledRemine = true;
        }
    }
}
