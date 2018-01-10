package com.aitusoftware.network.patterns.app;

import com.aitusoftware.network.patterns.config.Messages;
import com.aitusoftware.network.patterns.measurement.LatencyRecorder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public final class SingleThreadedRequestClient
{
    private final ReadableByteChannel inputChannel;
    private final WritableByteChannel outputChannel;
    private final ByteBuffer payload;
    private final long warmupMessages;
    private final long measurementMessages;
    private final LatencyRecorder latencyRecorder;

    SingleThreadedRequestClient(
            final ReadableByteChannel inputChannel, final WritableByteChannel outputChannel,
            final int payloadSize, final LatencyRecorder latencyRecorder,
            final long warmupMessages, final long measurementMessages)
    {
        this.inputChannel = inputChannel;
        this.outputChannel = outputChannel;
        this.payload = ByteBuffer.allocateDirect(payloadSize);
        this.latencyRecorder = latencyRecorder;
        this.warmupMessages = warmupMessages;
        this.measurementMessages = measurementMessages;
    }

    void sendLoop()
    {
        final long histogramClearInterval = warmupMessages / 500;
        long warmUpMessagesRemaining = warmupMessages;
        long totalMessagesRemaining = warmupMessages + measurementMessages;
        long sequenceNumber = 0;
        Thread.currentThread().setName(getClass().getSimpleName() + "-sendLoop");

        while (!Thread.currentThread().isInterrupted() && totalMessagesRemaining-- != 0)
        {
            try
            {
                Messages.setRequestData(payload, System.nanoTime(), sequenceNumber);
                while (payload.remaining() != 0)
                {
                    outputChannel.write(payload);
                }
                payload.clear();
                // TODO cannot handle dropped message - implement timeout
                while (payload.remaining() != 0)
                {
                    inputChannel.read(payload);
                }
                final long receivedSequence = Messages.retrieveSequence(payload);
                latencyRecorder.recordValue(System.nanoTime() - Messages.retrievePayload(payload));
                if (warmUpMessagesRemaining != 0)
                {
                    warmUpMessagesRemaining--;
                    if (warmUpMessagesRemaining % histogramClearInterval == 0)
                    {
                        latencyRecorder.reset();
                    }
                }
            }
            catch (IOException e)
            {
                System.err.printf("Failed to make request: %s. Exiting.%n", e.getMessage());
            }
        }

        latencyRecorder.complete();
    }
}