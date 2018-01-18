package com.aitusoftware.network.patterns.app;

import com.aitusoftware.network.patterns.config.Constants;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public final class UdpHandshake
{
    DatagramChannel initiateSend(final InetSocketAddress remote)
    {
        final long timeoutAt = System.currentTimeMillis() + Constants.CONNECT_TIMEOUT_MILLIS;
        while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < timeoutAt)
        {
            try
            {
                final DatagramChannel channel = DatagramChannel.open();
                channel.connect(remote);
                channel.configureBlocking(false);
                final ByteBuffer payload = ByteBuffer.allocateDirect(8);
                while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < timeoutAt)
                {
                    payload.clear();
                    payload.putLong(0, 0xFACE0FF);
                    Io.sendAll(payload, channel);
                    // wait for response to know that we're connected
                    payload.clear();
                    for (int i = 0; i < 100; i++)
                    {
                        if (channel.read(payload) != 0)
                        {
                            return channel;
                        }
                        idle();
                    }
                }
            }
            catch (IOException e)
            {
                // server not yet listening
            }
        }
        throw new UncheckedIOException(
                new IOException("Failed to complete handshake with remote: " + remote));
    }

    void performReceive(final DatagramChannel listenChannel)
    {
        final long timeoutAt = System.currentTimeMillis() + Constants.CONNECT_TIMEOUT_MILLIS;
        while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < timeoutAt)
        {
            try
            {
                listenChannel.configureBlocking(false);
                final ByteBuffer payload = ByteBuffer.allocateDirect(8);
                payload.clear();
                final SocketAddress remoteClient = listenChannel.receive(payload);

                if (remoteClient == null)
                {
                    idle();
                }
                else
                {
                    payload.flip();
                    if (payload.getLong(0) == 0xFACE0FF)
                    {
                        listenChannel.connect(remoteClient);
                        payload.clear();
                        Io.sendAll(payload, listenChannel);
                        return;
                    }
                }
            }
            catch (IOException e)
            {
                // failed to read
            }
        }
        throw new UncheckedIOException(
                new IOException("Failed to complete handshake with listener: " +
                        listenChannel.socket().getLocalAddress()));
    }

    private static void idle()
    {
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10L));
    }
}
