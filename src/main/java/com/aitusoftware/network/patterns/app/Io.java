package com.aitusoftware.network.patterns.app;

import java.io.Closeable;
import java.io.IOException;

public final class Io
{
    private Io() {}

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
