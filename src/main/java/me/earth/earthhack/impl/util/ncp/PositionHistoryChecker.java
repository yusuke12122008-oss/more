package me.earth.earthhack.impl.util.ncp;


import java.util.Deque;

public abstract class PositionHistoryChecker {
    // protected boolean invalidateFailed = true; TODO: implement
    protected boolean checkOldLook = true;
    protected int ticksToCheck = 10;

    protected abstract boolean check(double x, double y, double z,
                                     float yaw, float pitch,
                                     int blockX, int blockY, int blockZ);

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean checkFlyingQueue(double x, double y, double z,
                                    float oldYaw, float oldPitch,
                                    int blockX, int blockY, int blockZ) {
        if (checkOldLook) { // isPositionValid in RotationHistory
            if (check(x, y, z, oldYaw, oldPitch, blockX, blockY, blockZ)) {
                return true;
            }
        }

        return false;
    }

}
