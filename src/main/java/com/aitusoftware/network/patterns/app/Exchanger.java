package com.aitusoftware.network.patterns.app;

import com.aitusoftware.network.patterns.measurement.Timer;

import java.util.concurrent.atomic.AtomicLong;

public final class Exchanger
{
    private static final long UNSET_VALUE = Long.MIN_VALUE;

    private final AtomicLong container = new AtomicLong(UNSET_VALUE);
    private final Timer timer;

    public Exchanger(final Timer timer)
    {
        this.timer = timer;
    }

    public void set(final long value)
    {
        while (!container.compareAndSet(UNSET_VALUE, value) && timer.isBeforeDeadline())
        {
            if (Thread.currentThread().isInterrupted())
            {
                throw new RuntimeException("Bailing out");
            }
        }
    }

    public long get()
    {
        long value;
        while ((value = container.get()) == UNSET_VALUE && timer.isBeforeDeadline())
        {
            if (Thread.currentThread().isInterrupted())
            {
                throw new RuntimeException("Bailing out");
            }
        }
        container.lazySet(UNSET_VALUE);
        return value;
    }

    public static boolean isUnset(final long value)
    {
        return value == UNSET_VALUE;
    }
}