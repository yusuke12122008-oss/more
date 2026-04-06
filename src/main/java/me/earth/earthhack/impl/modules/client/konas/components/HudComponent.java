package me.earth.earthhack.impl.modules.client.konas.components;

import me.earth.earthhack.api.util.interfaces.Globals;
import me.earth.earthhack.impl.modules.client.konas.KonasHUD;

/**
 * Base class for all KonasHUD components.
 * Each component is a self-contained HUD element with its own
 * position, size, and render logic.
 */
public abstract class HudComponent implements Globals
{
    protected final KonasHUD hud;

    // Component name shown in settings
    private final String name;

    // Whether this component is currently visible
    private boolean visible;

    public HudComponent(KonasHUD hud, String name, boolean defaultVisible)
    {
        this.hud    = hud;
        this.name   = name;
        this.visible = defaultVisible;
    }

    /**
     * Called every frame when the HUD is active and this component is visible.
     *
     * @param sw  scaled screen width
     * @param sh  scaled screen height
     */
    public abstract void render(int sw, int sh);

    // ------------------------------------------------------------------ //

    public String getName()   { return name;    }
    public boolean isVisible(){ return visible; }
    public void setVisible(boolean v){ this.visible = v; }
}
