package me.earth.earthhack.impl.modules.combat.autofeettrap;

import me.earth.earthhack.impl.event.events.misc.TickEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;

final class ListenerTick extends ModuleListener<AutoFeetTrap, TickEvent>
{
    ListenerTick(AutoFeetTrap module)
    {
        super(module, TickEvent.class);
    }

    @Override
    public void invoke(TickEvent event)
    {
        module.onTick();
    }
}
