package com.aitusoftware.network.patterns.measurement;

import java.util.concurrent.TimeUnit;

public final class Timer
{
    private final long deadLine;

    private Timer(final long duration, final TimeUnit unit)
    {
        this.deadLine = System.nanoTime() + unit.toNanos(duration);
    }

    public boolean isBeforeDeadline()
    {
        return System.nanoTime() < deadLine;
    }

    public static Timer expiringIn(final long duration, final TimeUnit unit)
    {
        return new Timer(duration, unit);
    }
}
