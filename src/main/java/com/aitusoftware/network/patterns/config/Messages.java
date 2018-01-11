package com.aitusoftware.network.patterns.config;

import java.nio.ByteBuffer;

public final class Messages
{
    private Messages() {}

    public static void setRequestData(final ByteBuffer target, final int timestamp, final int sequence)
    {
        target.clear();
        target.putInt(0, timestamp);
        target.putInt(4, sequence);
    }

    public static int retrieveTimestamp(final ByteBuffer source)
    {
        return source.getInt(0);
    }

    public static int retrieveSequence(final ByteBuffer source)
    {
        return source.getInt(4);
    }
}