package com.aitusoftware.network.patterns.config;

public enum Connection
{
    BLOCKING(true),
    NON_BLOCKING(false);

    private final boolean blocking;

    Connection(final boolean blocking)
    {
        this.blocking = blocking;
    }

    public boolean isBlocking()
    {
        return blocking;
    }
}
