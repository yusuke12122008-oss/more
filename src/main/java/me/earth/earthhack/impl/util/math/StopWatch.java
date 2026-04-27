package me.earth.earthhack.impl.util.math;

public class StopWatch implements Passable
{
    private volatile long time;

    public boolean passed(double ms)
    {
        return System.currentTimeMillis() - time >= ms;
    }

    @Override
    public boolean passed(long ms)
    {
        return System.currentTimeMillis() - time >= ms;
    }

    public StopWatch reset()
    {
        time = System.currentTimeMillis();
        return this;
    }

    /**
     * Compatibility overload for call-sites that still pass a delay argument.
     * The delay itself is checked in {@link #passed(long)}, so reset always
     * restarts the watch from "now".
     */
    public StopWatch reset(long ignored)
    {
        return reset();
    }

    public long getTime()
    {
        return System.currentTimeMillis() - time;
    }

    public void setTime(long ns)
    {
        time = ns;
    }

}
