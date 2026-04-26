package me.earth.earthhack.impl.util.helpers.blocks.attack;

import me.earth.earthhack.api.event.bus.EventListener;
import me.earth.earthhack.api.event.bus.instance.Bus;
import me.earth.earthhack.api.event.events.Stage;
import me.earth.earthhack.impl.event.events.network.MotionUpdateEvent;
import me.earth.earthhack.impl.util.math.rotation.RotationUtil;
import me.earth.earthhack.impl.util.minecraft.DamageUtil;
import me.earth.earthhack.impl.util.minecraft.entity.EntityUtil;
import me.earth.earthhack.impl.util.network.PacketUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;

import java.util.List;

public class GlobalAttackHelper implements AttackHelper
{
    private static final GlobalAttackHelper INSTANCE = new GlobalAttackHelper();
    private boolean noCrystal;
    private boolean attacked;
    private boolean invalid;

    private GlobalAttackHelper()
    {
        Bus.EVENT_BUS.register(new EventListener<MotionUpdateEvent>
                (MotionUpdateEvent.class, Integer.MIN_VALUE)
        {
            @Override
            public void invoke(MotionUpdateEvent event)
            {
                if (event.getStage() == Stage.POST)
                {
                    attacked = false;
                    noCrystal = false;
                    invalid = false;
                }
            }
        });
    }

    public static GlobalAttackHelper getInstance()
    {
        return INSTANCE;
    }

    @Override
    public boolean attackAny(List<Entity> entities, AttackingModule module)
    {
        return false;
    }

    public void setAttacked(boolean attacked)
    {
        this.attacked = attacked;
    }

}
