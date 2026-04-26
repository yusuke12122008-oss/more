package me.earth.earthhack.impl.modules.combat.crystalaura;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;

final class ListenerTick extends ModuleListener<CrystalAura, TickEvent>
{
    ListenerTick(CrystalAura module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        module.runThreadedTick();
    }
}
