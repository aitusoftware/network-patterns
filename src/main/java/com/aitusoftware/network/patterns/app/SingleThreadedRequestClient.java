package com.aitusoftware.network.patterns.app;

import com.aitusoftware.network.patterns.config.Messages;
import com.aitusoftware.network.patterns.measurement.LatencyRecorder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.TimeUnit;

public final class SingleThreadedRequestClient
{
    private static final long TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(1L);
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
        payload = ByteBuffer.allocateDirect(payloadSize);
        this.latencyRecorder = latencyRecorder;
        this.warmupMessages = warmupMessages;
        this.measurementMessages = measurementMessages;
    }

    void sendLoop()
    {
        final long histogramClearInterval = warmupMessages / 500;
        long warmUpMessagesRemaining = warmupMessages;
        long totalMessagesRemaining = warmupMessages + measurementMessages;
        int sequenceNumber = 0;
        Thread.currentThread().setName(getClass().getSimpleName() + "-sendLoop");
        final long startNanos = System.nanoTime();

        while (!Thread.currentThread().isInterrupted() && totalMessagesRemaining-- != 0)
        {
            warmUpMessagesRemaining =
                    recordSingleMessageLatency(histogramClearInterval, warmUpMessagesRemaining,
                            sequenceNumber, startNanos);
        }

        latencyRecorder.complete();
    }

    private long recordSingleMessageLatency(final long histogramClearInterval, long warmUpMessagesRemaining, final int sequenceNumber, final long startNanos)
    {
        try
        {
            sendMessage(sequenceNumber, startNanos);
            receiveMessage();
            recordLatency(sequenceNumber, startNanos);
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
        return warmUpMessagesRemaining;
    }

    private void recordLatency(final int sequenceNumber, final long startNanos)
    {
        if (payload.remaining() != 0)
        {
            latencyRecorder.messagesDropped(1);
        }
        final int receivedTime = (int) (System.nanoTime() - startNanos);
        final int receivedSequence = Messages.retrieveSequence(payload);
        if (receivedSequence != sequenceNumber)
        {
            latencyRecorder.messagesDropped(sequenceNumber - receivedSequence);
        }
        else
        {
            latencyRecorder.recordValue(receivedTime -
                    Messages.retrieveTimestamp(payload));
        }
    }

    private void receiveMessage() throws IOException
    {
        payload.clear();
        final long responseTimeout = System.nanoTime() + TIMEOUT_NANOS;
        while (payload.remaining() != 0 && System.nanoTime() < responseTimeout)
        {
            inputChannel.read(payload);
        }
    }

    private void sendMessage(final int sequenceNumber, final long startNanos) throws IOException
    {
        payload.clear();
        final int sendingTime = (int) (System.nanoTime() - startNanos);
        Messages.setRequestData(payload, sendingTime, sequenceNumber);
        while (payload.remaining() != 0)
        {
            outputChannel.write(payload);
        }
    }
}