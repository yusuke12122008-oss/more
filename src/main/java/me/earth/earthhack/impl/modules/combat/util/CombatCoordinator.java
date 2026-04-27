package me.earth.earthhack.impl.modules.combat.util;

import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatCoordinator
{
    private static final Set<BlockPos> FEET_TARGETS =
        Collections.newSetFromMap(new ConcurrentHashMap<BlockPos, Boolean>());

    private static final Set<BlockPos> CRYSTAL_PLACE_TARGETS =
        Collections.newSetFromMap(new ConcurrentHashMap<BlockPos, Boolean>());

    private static final Map<Integer, Long> ATTACKED_CRYSTALS =
        new ConcurrentHashMap<>();

    private static volatile boolean feetTrapActive;
    private static volatile boolean feetTrapPlacing;
    private static volatile boolean autoCrystalActive;
    private static volatile boolean autoCrystalPlacing;

    private CombatCoordinator()
    {
        throw new AssertionError("No instances.");
    }

    public static void setFeetTrapActive(boolean active)
    {
        feetTrapActive = active;

        if (!active)
        {
            feetTrapPlacing = false;
            FEET_TARGETS.clear();
        }
    }

    public static void setAutoCrystalActive(boolean active)
    {
        autoCrystalActive = active;

        if (!active)
        {
            autoCrystalPlacing = false;
            CRYSTAL_PLACE_TARGETS.clear();
        }
    }

    public static void setFeetTrapPlacing(boolean placing)
    {
        feetTrapPlacing = placing;
    }

    public static void setAutoCrystalPlacing(boolean placing)
    {
        autoCrystalPlacing = placing;
    }

    public static boolean isFeetTrapActive()
    {
        return feetTrapActive;
    }

    public static boolean isFeetTrapPlacing()
    {
        return feetTrapPlacing;
    }

    public static boolean isAutoCrystalActive()
    {
        return autoCrystalActive;
    }

    public static boolean isAutoCrystalPlacing()
    {
        return autoCrystalPlacing;
    }

    public static void updateFeetTargets(Set<BlockPos> targets)
    {
        FEET_TARGETS.clear();

        if (targets != null)
        {
            FEET_TARGETS.addAll(targets);
        }
    }

    public static void updateCrystalPlaceTargets(Set<BlockPos> targets)
    {
        CRYSTAL_PLACE_TARGETS.clear();

        if (targets != null)
        {
            CRYSTAL_PLACE_TARGETS.addAll(targets);
        }
    }

    public static Set<BlockPos> getFeetTargets()
    {
        return new HashSet<>(FEET_TARGETS);
    }

    public static Set<BlockPos> getCrystalPlaceTargets()
    {
        return new HashSet<>(CRYSTAL_PLACE_TARGETS);
    }

    public static boolean isFeetTarget(BlockPos pos)
    {
        return pos != null && FEET_TARGETS.contains(pos);
    }

    public static boolean isCrystalPlaceTarget(BlockPos pos)
    {
        return pos != null && CRYSTAL_PLACE_TARGETS.contains(pos);
    }

    public static boolean intersectsFeetTargets(BlockPos crystalBase)
    {
        if (crystalBase == null)
        {
            return false;
        }

        BlockPos crystalPos = crystalBase.up();

        for (BlockPos feet : FEET_TARGETS)
        {
            if (feet.equals(crystalBase)
                || feet.equals(crystalPos)
                || feet.distanceSq(crystalPos) <= 2.0)
            {
                return true;
            }
        }

        return false;
    }

    public static void markCrystalAttacked(int id)
    {
        ATTACKED_CRYSTALS.put(id, System.currentTimeMillis());
    }

    public static boolean wasCrystalRecentlyAttacked(int id, long timeout)
    {
        Long time = ATTACKED_CRYSTALS.get(id);

        return time != null
            && System.currentTimeMillis() - time <= timeout;
    }

    public static void clean()
    {
        long now = System.currentTimeMillis();

        ATTACKED_CRYSTALS.entrySet().removeIf(e -> now - e.getValue() > 1500L);
    }
}