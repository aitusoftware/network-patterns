package com.aitusoftware.network.patterns.app;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public final class Io
{
    private Io() {}

    static void sendAll(final ByteBuffer payload, final WritableByteChannel output) throws IOException
    {
        output.write(payload);
        if (payload.remaining() != 0)
        {
            while (payload.remaining() != 0)
            {
                output.write(payload);
            }
        }
    }

    public static void closeQuietly(final Closeable closeable)
    {
        if (closeable != null)
        {
            try
            {
                closeable.close();
            }
            catch (IOException e)
            {
                // no-op
            }
        }
    }
}
