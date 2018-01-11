package com.aitusoftware.network.patterns.app;

import java.util.concurrent.atomic.AtomicLong;

public final class Exchanger
{
    private static final long UNSET_VALUE = Long.MIN_VALUE;

    private final AtomicLong container = new AtomicLong(UNSET_VALUE);

    public void set(final long value)
    {
        while (!container.compareAndSet(UNSET_VALUE, value))
        {
            // spin
        }
    }

    public long get()
    {
        long value;
        while ((value = container.get()) == UNSET_VALUE)
        {
            // spin
        }
        container.lazySet(UNSET_VALUE);
        return value;
    }
}