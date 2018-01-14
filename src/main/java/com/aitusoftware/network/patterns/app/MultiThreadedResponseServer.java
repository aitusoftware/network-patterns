package com.aitusoftware.network.patterns.app;

import com.aitusoftware.network.patterns.config.Constants;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public final class MultiThreadedResponseServer
{
    private final ReadableByteChannel inputChannel;
    private final WritableByteChannel outputChannel;
    private final ByteBuffer requestBuffer;
    private final ByteBuffer responseBuffer;
    private final Exchanger exchanger = new Exchanger();
    private long remainingMessages = Constants.MEASUREMENT_MESSAGES + Constants.WARMUP_MESSAGES;
    private volatile Thread responseThread;

    MultiThreadedResponseServer(
            final ReadableByteChannel inputChannel, final WritableByteChannel outputChannel,
            final int payloadSize)
    {
        this.inputChannel = inputChannel;
        this.outputChannel = outputChannel;
        this.requestBuffer = ByteBuffer.allocateDirect(payloadSize);
        this.responseBuffer = ByteBuffer.allocateDirect(payloadSize);
    }

    void receiveLoop()
    {
        Thread.currentThread().setName(getClass().getSimpleName() + "-receiveLoop");
        while (!Thread.currentThread().isInterrupted())
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
                    remainingMessages--;
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
            if (remainingMessages == 0)
            {
                closeChannels();
                return;
            }
        }
    }

    void responseLoop()
    {
        responseThread = Thread.currentThread();
        Thread.currentThread().setName(getClass().getSimpleName() + "-respondLoop");
        while (!Thread.currentThread().isInterrupted())
        {
            final long echoPayload = exchanger.get();

            try
            {
                responseBuffer.clear();
                responseBuffer.putLong(0, echoPayload);
                while (responseBuffer.remaining() != 0)
                {
                    outputChannel.write(responseBuffer);
                }
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