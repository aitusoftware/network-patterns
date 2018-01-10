package com.aitusoftware.network.patterns.config;

import java.nio.ByteBuffer;

public final class Messages
{
    private Messages() {}

    public static void setRequestData(final ByteBuffer target, final long payload, final long sequence)
    {
        target.clear();
        target.putLong(0, payload);
        target.putLong(8, sequence);
    }

    public static long retrievePayload(final ByteBuffer source)
    {
        return source.getLong(0);
    }

    public static long retrieveSequence(final ByteBuffer source)
    {
        return source.getLong(8);
    }
}