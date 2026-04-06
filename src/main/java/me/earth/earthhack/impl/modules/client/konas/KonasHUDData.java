package me.earth.earthhack.impl.modules.client.konas;

import me.earth.earthhack.api.module.data.DefaultData;

public class KonasHUDData extends DefaultData<KonasHUD>
{
    public KonasHUDData(KonasHUD module)
    {
        super(module);
    }

    @Override
    public int getColor()
    {
        return 0xFF64C8FF;
    }

    @Override
    public String getDescription()
    {
        return "A clean, modular HUD system inspired by Konas / KonasClient. "
             + "Features: ArrayList, Info (FPS/Ping/TPS/Speed), Coordinates, Armor bars, Totem counter.";
    }
}
