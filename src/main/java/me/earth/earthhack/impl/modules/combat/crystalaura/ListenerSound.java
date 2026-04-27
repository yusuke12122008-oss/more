package me.earth.earthhack.impl.modules.combat.crystalaura;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;

final class ListenerSound extends ModuleListener<CrystalAura, PacketEvent.Receive<SPacketSoundEffect>>
{
    ListenerSound(CrystalAura module)
    {
        super(module, PacketEvent.Receive.class, SPacketSoundEffect.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketSoundEffect> event)
    {
        if (mc.player == null || mc.world == null || !module.soundRemove.getValue())
        {
            return;
        }

        SPacketSoundEffect packet = event.getPacket();
        if (packet.getCategory() == SoundCategory.BLOCKS
            && packet.getSound() == SoundEvents.ENTITY_GENERIC_EXPLODE)
        {
            synchronized (module.stateLock)
            {
                BlockPos exploded = new BlockPos(packet.getX(), packet.getY(), packet.getZ()).down();
                module.positionCache.markExploded(exploded.up());
                if (module.endSequenceOnExplosion.getValue())
                {
                    module.clearSequence();
                }
            }
        }
    }
}
