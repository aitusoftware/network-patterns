package com.aitusoftware.network.patterns.app;

import com.aitusoftware.network.patterns.config.Messages;
import com.aitusoftware.network.patterns.measurement.LatencyRecorder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public final class MultiThreadedRequestClient
{
    private static final long TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(1L);
    private static final long BASE_TIMESTAMP = System.nanoTime();
    private final ReadableByteChannel inputChannel;
    private final WritableByteChannel outputChannel;
    private final ByteBuffer requestBuffer;
    private final ByteBuffer responseBuffer;
    private final long warmupMessages;
    private final long measurementMessages;
    private final LatencyRecorder latencyRecorder;
    private final Exchanger exchanger = new Exchanger();

    MultiThreadedRequestClient(
            final ReadableByteChannel inputChannel, final WritableByteChannel outputChannel,
            final int payloadSize, final LatencyRecorder latencyRecorder,
            final long warmupMessages, final long measurementMessages)
    {
        this.inputChannel = inputChannel;
        this.outputChannel = outputChannel;
        requestBuffer = ByteBuffer.allocateDirect(payloadSize);
        responseBuffer = ByteBuffer.allocateDirect(payloadSize);
        this.latencyRecorder = latencyRecorder;
        this.warmupMessages = warmupMessages;
        this.measurementMessages = measurementMessages;
    }

    void sendLoop()
    {
        final long histogramClearInterval = warmupMessages / 500;
        long warmUpMessagesRemaining = warmupMessages;
        long totalMessagesRemaining = warmupMessages + measurementMessages;
        short sequenceNumber = 0;
        Thread.currentThread().setName(getClass().getSimpleName() + "-sendLoop");
        final long startNanos = System.nanoTime();

        try
        {
            while (!Thread.currentThread().isInterrupted() && totalMessagesRemaining-- != 0)
            {
                try
                {
                    sendMessage(sequenceNumber, startNanos);

                    recordLatency(sequenceNumber, startNanos);
                    if (warmUpMessagesRemaining != 0)
                    {
                        warmUpMessagesRemaining--;
                        if (warmUpMessagesRemaining % histogramClearInterval == 0)
                        {
                            latencyRecorder.reset();
                        }
                    }
                    sequenceNumber++;
                }
                catch (IOException e)
                {
                    closeConnections();
                    System.err.printf("Failed to make request: %s. Exiting.%n", e.getMessage());
                }
            }
        }
        finally
        {
            closeConnections();
        }

        latencyRecorder.complete();
    }

    void receiveLoop()
    {
        Thread.currentThread().setName(getClass().getSimpleName() + "-receiveLoop");
        while (!Thread.currentThread().isInterrupted())
        {
            try
            {
                final int read = inputChannel.read(responseBuffer);
                if (read == -1)
                {
                    closeConnections();
                    return;
                }
                if (responseBuffer.remaining() == 0)
                {
                    responseBuffer.flip();
                    exchanger.set(responseBuffer.getLong(0));
                    responseBuffer.clear();
                }
            }
            catch (IOException e)
            {
                closeConnections();
                return;
            }
        }
    }

    private void recordLatency(final short sequenceNumber, final long startNanos)
    {
        final long response = exchanger.get();

        final long receivedTime = Messages.trimmedTimestamp(System.nanoTime(), BASE_TIMESTAMP);
        final short receivedSequence = (short) (response >> 48);

        if (receivedSequence != sequenceNumber)
        {
            latencyRecorder.messagesDropped(sequenceNumber - receivedSequence);
        }
        else
        {
            final long latencyNanos = receivedTime - Messages.maskTimestamp(response);
            if (latencyNanos < 0)
            {
                latencyRecorder.recordValue(latencyNanos + Integer.MAX_VALUE);
                System.out.printf("at %s %d, received: %d, sent: %d, latency: %d%n",
                        Instant.now(),
                        System.nanoTime(),
                        (receivedTime & TIMESTAMP_MASK), response >> 32, latencyNanos);
            }
            else
            {
                latencyRecorder.recordValue(latencyNanos);
            }
        }
    }

    private void closeConnections()
    {
        Io.closeQuietly(inputChannel);
        Io.closeQuietly(outputChannel);
    }

    private static final long TIMESTAMP_MASK = 0b00000000_00000000_01111111_11111111_11111111_11111111_11111111_11111111L;

    private void sendMessage(final short sequenceNumber, final long startNanos) throws IOException
    {
        requestBuffer.clear();
        final long sendingTime = Messages.trimmedTimestamp(System.nanoTime(), BASE_TIMESTAMP);

        Messages.setRequestDataSinglePayload(requestBuffer, sendingTime | ((long) sequenceNumber) << 48);
        while (requestBuffer.remaining() != 0)
        {
            outputChannel.write(requestBuffer);
        }
    }
}