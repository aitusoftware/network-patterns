package com.aitusoftware.network.patterns.app;

import com.aitusoftware.network.patterns.config.Constants;
import com.aitusoftware.network.patterns.measurement.Timer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public final class SimpleResponseServer
{
    private final ReadableByteChannel inputChannel;
    private final WritableByteChannel outputChannel;
    private final ByteBuffer requestBuffer;
    private final ByteBuffer responseBuffer;
    private final Exchanger exchanger;
    private final Timer timer;
    private volatile Thread responseThread;

    SimpleResponseServer(
            final ReadableByteChannel inputChannel, final WritableByteChannel outputChannel,
            final int payloadSize)
    {
        this.inputChannel = inputChannel;
        this.outputChannel = outputChannel;
        this.requestBuffer = ByteBuffer.allocateDirect(payloadSize);
        this.responseBuffer = ByteBuffer.allocateDirect(payloadSize);
        this.timer = Timer.expiringIn(Constants.RUNTIME_MINUTES, TimeUnit.MINUTES);
        exchanger = new Exchanger(timer);
    }

    void receiveLoop()
    {
        ThreadAffinity.setThreadAffinity(ThreadAffinity.ThreadId.SERVER_INBOUND);
        System.out.printf("Starting response server at %s%n", Instant.now());
        Thread.currentThread().setName(getClass().getSimpleName() + "-receiveLoop");
        try
        {
            while (!Thread.currentThread().isInterrupted() && timer.isBeforeDeadline())
            {
                try
                {
                    final int read = inputChannel.read(requestBuffer);
                    if (read == -1)
                    {
                        closeChannels();
                        return;
                    }
                    if (requestBuffer.remaining() == 0)
                    {
                        requestBuffer.flip();
                        exchanger.set(requestBuffer.getLong(0));
                        requestBuffer.clear();
                    }
                }
                catch (IOException e)
                {
                    System.err.printf("Failed to receive request: %s. Exiting.%n", e.getMessage());
                    return;
                }
            }
        }
        finally
        {
            closeChannels();
            System.out.printf("Response loop complete at %s%n", Instant.now());
        }
    }

    void responseLoop()
    {
        ThreadAffinity.setThreadAffinity(ThreadAffinity.ThreadId.SERVER_OUTBOUND);
        responseThread = Thread.currentThread();
        Thread.currentThread().setName(getClass().getSimpleName() + "-respondLoop");
        while (!Thread.currentThread().isInterrupted() && timer.isBeforeDeadline())
        {
            final long echoPayload = exchanger.get();

            try
            {
                responseBuffer.clear();
                responseBuffer.putLong(0, echoPayload);
                Io.sendAll(responseBuffer, outputChannel);
            }
            catch (IOException e)
            {
                closeChannels();
                System.err.printf("Failed to respond to request: %s. Exiting.%n", e.getMessage());
                return;
            }
        }
    }

    private void closeChannels()
    {
        Io.closeQuietly(inputChannel);
        Io.closeQuietly(outputChannel);
        if (responseThread != null)
        {
            responseThread.interrupt();
        }
    }
}