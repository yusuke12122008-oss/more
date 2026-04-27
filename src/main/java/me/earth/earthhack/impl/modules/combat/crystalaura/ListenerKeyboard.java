package me.earth.earthhack.impl.modules.combat.crystalaura;

import me.earth.earthhack.impl.event.events.keyboard.KeyboardEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;

final class ListenerKeyboard extends ModuleListener<CrystalAura, KeyboardEvent>
{
    ListenerKeyboard(CrystalAura module)
    {
        super(module, KeyboardEvent.class);
    }

    @Override
    public void invoke(KeyboardEvent event)
    {
        int bind = module.switchBind.getValue().getKey();
        if (bind != -1
            && event.getEventState()
            && event.getKey() == bind
            && module.autoSwitch.getValue() == CrystalAura.AutoSwitchMode.Bind)
        {
            synchronized (module.stateLock)
            {
                module.switching = !module.switching;
            }
        }
    }
}
