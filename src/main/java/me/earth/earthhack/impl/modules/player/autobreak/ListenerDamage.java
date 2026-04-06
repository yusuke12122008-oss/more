package me.earth.earthhack.impl.modules.player.autobreak;

import me.earth.earthhack.impl.event.events.misc.DamageBlockEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.minecraft.blocks.mine.MineUtil;
import net.minecraft.util.EnumHand;

public class ListenerDamage extends ModuleListener<AutoBreak, DamageBlockEvent> {
    public ListenerDamage(AutoBreak module) {
        super(module, DamageBlockEvent.class);
    }

    @Override
    public void invoke(DamageBlockEvent event) {
        if (!MineUtil.canBreak(event.getPos())) {
            return;
        }

        if (module.mining != null && module.mining.equals(event.getPos())) {
            module.facing = event.getFacing() != null ? event.getFacing() : module.facing;
            module.restartCurrentTarget();
            event.setCancelled(true);
            return;
        }

        if (module.mining != null) {
            module.abortCurrentPos();
        }

        module.stopPos = null;
        module.lastConfirmedBroke = null;
        module.scheduledRemine = true;
        module.startMining(event.getPos(), event.getFacing());

        mc.player.swingArm(EnumHand.MAIN_HAND);
        event.setCancelled(true);
    }
}
