package com.aitusoftware.network.patterns.app;

import com.aitusoftware.network.patterns.config.Constants;
import com.aitusoftware.network.patterns.measurement.Timer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public final class SingleThreadedResponseServer
{
    private final ReadableByteChannel inputChannel;
    private final WritableByteChannel outputChannel;
    private final ByteBuffer payload;
    private final Timer timer;
    private long received;

    SingleThreadedResponseServer(
            final ReadableByteChannel inputChannel, final WritableByteChannel outputChannel,
            final int payloadSize)
    {
        this.inputChannel = inputChannel;
        this.outputChannel = outputChannel;
        this.payload = ByteBuffer.allocateDirect(payloadSize);
        this.timer = Timer.expiringIn(Constants.RUNTIME_MINUTES, TimeUnit.MINUTES);
    }

    public void receiveLoop()
    {
        ThreadAffinity.setThreadAffinity(ThreadAffinity.ThreadId.SERVER_INBOUND);
        System.out.printf("Starting response server at %s%n", Instant.now());
        Thread.currentThread().setName(getClass().getSimpleName() + "-receiveLoop");
        try
        {
            while (!Thread.currentThread().isInterrupted() && received < 10_000_000)
            {
                try
                {
                    final int read = inputChannel.read(payload);
                    if (read == -1)
                    {
                        Io.closeQuietly(inputChannel);
                        Io.closeQuietly(outputChannel);
                        break;
                    }
                    if (payload.remaining() == 0)
                    {
                        payload.flip();
                        Io.sendAll(payload, outputChannel);
                        payload.clear();
                        received++;
                    }
                }
                catch (IOException e)
                {
                    System.err.printf("Failed to respond to request: %s. Exiting.%n", e.getMessage());
                    break;
                }
            }
//            while (timer.isBeforeDeadline())
//            {
//                LockSupport.parkNanos(1);
//            }
            Io.closeQuietly(inputChannel);
            Io.closeQuietly(outputChannel);
        }
        finally
        {
            System.out.printf("Response loop complete at %s%n", Instant.now());
        }
    }
}