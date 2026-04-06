package me.earth.earthhack.impl.modules.client.konas;

import me.earth.earthhack.impl.event.events.render.Render2DEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

/**
 * Listens for the 2D render event and calls KonasHUD#render.
 */
final class KonasListenerRender extends ModuleListener<KonasHUD, Render2DEvent>
{
    public KonasListenerRender(KonasHUD module)
    {
        super(module, Render2DEvent.class);
    }

    @Override
    public void invoke(Render2DEvent event)
    {
        GL11.glPushMatrix();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        ScaledResolution sr = new ScaledResolution(mc);
        module.render(sr.getScaledWidth(), sr.getScaledHeight());

        GL11.glPopMatrix();
    }
}
