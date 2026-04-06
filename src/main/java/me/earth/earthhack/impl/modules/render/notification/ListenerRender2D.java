package me.earth.earthhack.impl.modules.render.notification;

import me.earth.earthhack.impl.event.events.render.Render2DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import org.lwjgl.opengl.GL11;

final class ListenerRender2D
        extends ModuleListener<Notification, Render2DEvent> {

    public ListenerRender2D(Notification module) {
        super(module, Render2DEvent.class);
    }

    @Override
    public void invoke(Render2DEvent event) {
        GL11.glPushMatrix();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        module.getRenderer().onRender2D();
        GL11.glPopMatrix();
    }
}
