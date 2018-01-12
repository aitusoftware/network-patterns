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
            if (Thread.currentThread().isInterrupted())
            {
                throw new RuntimeException("Bailing out");
            }
        }
    }

    public long get()
    {
        // TODO add timeout, return UNSET_VALUE
        long value;
        while ((value = container.get()) == UNSET_VALUE)
        {
            if (Thread.currentThread().isInterrupted())
            {
                throw new RuntimeException("Bailing out");
            }
        }
        container.lazySet(UNSET_VALUE);
        return value;
    }
}