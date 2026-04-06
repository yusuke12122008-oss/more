package me.earth.earthhack.impl.modules.render.notification;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.SPacketEntityStatus;

import java.util.HashMap;
import java.util.Map;

final class ListenerPacketReceive
        extends ModuleListener<Notification, PacketEvent.Receive<SPacketEntityStatus>> {

    private final Map<String, Integer> popCounter = new HashMap<>();

    public ListenerPacketReceive(Notification module) {
        super(module, PacketEvent.Receive.class, SPacketEntityStatus.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketEntityStatus> event) {
        if (mc.world == null || mc.player == null) return;

        SPacketEntityStatus packet = event.getPacket();
        Entity entity = packet.getEntity(mc.world);
        if (!(entity instanceof EntityPlayer)) return;

        EntityPlayer player = (EntityPlayer) entity;

        // Opcode 35 = totem of undying activation
        if (packet.getOpCode() == 35 && module.totemPop.getValue()) {
            int pops = countPop(player.getName());
            if (pops == 1) {
                module.getRenderer().show(NotificationType.WARNING,
                        player.getName() + " popped a totem!");
            } else {
                module.getRenderer().show(NotificationType.WARNING,
                        player.getName() + " popped " + pops + " totems!");
            }
        }

        // Opcode 3 = entity death
        if (packet.getOpCode() == 3 && module.death.getValue()) {
            int pops = getPop(player.getName());
            String msg;
            if (pops == 0) {
                msg = player.getName() + " died!";
            } else {
                msg = player.getName() + " died after "
                        + pops + " pop" + (pops == 1 ? "!" : "s!");
            }
            module.getRenderer().show(NotificationType.ERROR, msg);
            popCounter.remove(player.getName());
        }
    }

    private int countPop(String name) {
        int current = popCounter.getOrDefault(name, 0) + 1;
        popCounter.put(name, current);
        return current;
    }

    private int getPop(String name) {
        return popCounter.getOrDefault(name, 0);
    }
}
