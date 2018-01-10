package com.aitusoftware.network.patterns.app;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

public final class SingleThreadedResponseServer
{
    private final ReadableByteChannel inputChannel;
    private final WritableByteChannel outputChannel;
    private final ByteBuffer payload;

    SingleThreadedResponseServer(
            final ReadableByteChannel inputChannel, final WritableByteChannel outputChannel,
            final int payloadSize)
    {
        this.inputChannel = inputChannel;
        this.outputChannel = outputChannel;
        this.payload = ByteBuffer.allocateDirect(payloadSize);
    }

    public void receiveLoop()
    {
        Thread.currentThread().setName(getClass().getSimpleName() + "-receiveLoop");
        while (!Thread.currentThread().isInterrupted())
        {
            try
            {
                inputChannel.read(payload);
                if (payload.remaining() == 0)
                {
                    payload.flip();
                    while (payload.remaining() != 0)
                    {
                        outputChannel.write(payload);
                    }
                    payload.clear();
                }
            }
            catch (IOException e)
            {
                System.err.printf("Failed to respond to request: %s. Exiting.%n", e.getMessage());
                return;
            }
        }
    }
}