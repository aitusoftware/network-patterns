package com.aitusoftware.network.patterns.config;

import java.nio.ByteBuffer;

public final class Messages
{
    private static final long TIMESTAMP_MASK = 0b00000000_00000000_01111111_11111111_11111111_11111111_11111111_11111111L;

    private Messages() {}

    public static void setRequestData(final ByteBuffer target, final int timestamp, final int sequence)
    {
        target.clear();
        target.putInt(0, timestamp);
        target.putInt(4, sequence);
    }

    public static long trimmedTimestamp(final long nanoTime, final long baseNanoTime)
    {
        return (nanoTime - baseNanoTime) & TIMESTAMP_MASK;
    }

    public static void setRequestDataSinglePayload(final ByteBuffer target,
                                                   final long payload)
    {
        target.clear();
        target.putLong(0, payload);
    }

    public static int retrieveTimestamp(final ByteBuffer source)
    {
        return source.getInt(0);
    }

    public static int retrieveSequence(final ByteBuffer source)
    {
        return source.getInt(4);
    }

    public static long maskTimestamp(final long response)
    {
        return response & TIMESTAMP_MASK;
    }
}