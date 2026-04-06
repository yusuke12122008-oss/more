package me.earth.earthhack.impl.modules.client.server.protocol.handlers;

import me.earth.earthhack.api.cache.ModuleCache;
import me.earth.earthhack.impl.modules.Caches;
import me.earth.earthhack.impl.modules.client.server.api.IConnection;
import me.earth.earthhack.impl.modules.client.server.api.IPacketHandler;


import java.io.IOException;

public class EatingHandler implements IPacketHandler
{


    @Override
    public void handle(IConnection connection, byte[] bytes) throws IOException
    {
        // TODO: since we write either Byte.MIN_VALUE or 0,
        //  we could check if more than half of the bits are != 1
        //  instead, would be more safe
        if (bytes[0] != 0)
        {
            // AUTO_EAT.computeIfPresent(a -> a.setServer(true));
        }
        else
        {
            // AUTO_EAT.computeIfPresent(a -> a.setServer(false));
        }
    }

}
