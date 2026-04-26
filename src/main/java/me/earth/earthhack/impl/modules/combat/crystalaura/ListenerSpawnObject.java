package me.earth.earthhack.impl.modules.combat.crystalaura;

import me.earth.earthhack.impl.event.events.network.PacketEvent;
import me.earth.earthhack.impl.event.listeners.ModuleListener;
import me.earth.earthhack.impl.util.network.PacketUtil;
import net.minecraft.network.play.server.SPacketSpawnObject;
import net.minecraft.util.math.BlockPos;

final class ListenerSpawnObject extends ModuleListener<CrystalAura, PacketEvent.Receive<SPacketSpawnObject>>
{
    ListenerSpawnObject(CrystalAura module)
    {
        super(module, PacketEvent.Receive.class, SPacketSpawnObject.class);
    }

    @Override
    public void invoke(PacketEvent.Receive<SPacketSpawnObject> event)
    {
        if (mc.player == null || mc.world == null)
        {
            return;
        }

        SPacketSpawnObject packet = event.getPacket();
        if (packet.getType() != 51)
        {
            return;
        }

        synchronized (module.stateLock)
        {
            BlockPos crystalPos = new BlockPos(packet.getX(), packet.getY(), packet.getZ());
            BlockPos base = crystalPos.down();
            module.positionCache.confirmPlace(crystalPos);

            if (module.expecting != null
                && module.expecting.equals(base)
                && module.endSequenceOnSpawn.getValue())
            {
                module.clearSequence();
            }

            if (module.predictSpawn.getValue()
                && module.breakTimer.passed(module.breakDelay.getValue())
                && module.canAttackSpawn(crystalPos, base))
            {
                PacketUtil.attack(packet.getEntityID());
                module.breakTimer.reset(module.breakDelay.getValue());
            }
        }
    }
}
