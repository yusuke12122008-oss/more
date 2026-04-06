package me.earth.earthhack.pingbypass.listeners;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.api.cache.SettingCache;
import me.earth.earthhack.api.event.bus.SubscriberImpl;
import me.earth.earthhack.api.module.Module;
import me.earth.earthhack.api.setting.Setting;
import me.earth.earthhack.impl.core.ducks.network.ICPacketPlayerDigging;
import me.earth.earthhack.impl.event.listeners.SendListener;

import me.earth.earthhack.pingbypass.PingBypass;
import net.minecraft.network.play.client.CPacketPlayerDigging;

public class ClientDiggingService extends SubscriberImpl {

    public ClientDiggingService() {
        this.listeners.add(new SendListener<>(CPacketPlayerDigging.class, e -> {
            if (PingBypassModule.CACHE.isEnabled()
                && !PingBypassModule.CACHE.get().isOld()
                && (e.getPacket().getAction() == CPacketPlayerDigging.Action.START_DESTROY_BLOCK
                || e.getPacket().getAction() == CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK
                || e.getPacket().getAction() == CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK)
                && ((ICPacketPlayerDigging) e.getPacket()).isNormalDigging()) {
                e.setCancelled(true);
            }
        }));
    }

}
