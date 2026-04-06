package me.earth.earthhack.impl.modules.player.autobreak;

import me.earth.earthhack.impl.event.events.misc.ClickBlockEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;

public class ListenerBlockClick extends ModuleListener<AutoBreak, ClickBlockEvent> {
    public ListenerBlockClick(AutoBreak module) {
        super(module, ClickBlockEvent.class);
    }

    @Override
    public void invoke(ClickBlockEvent event) {
        // Do not aggressively cancel left click here.
        // DamageBlockEvent handles target selection and restart logic.
    }
}
