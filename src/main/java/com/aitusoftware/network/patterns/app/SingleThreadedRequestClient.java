package com.aitusoftware.network.patterns.app;

import com.aitusoftware.network.patterns.config.Constants;
import com.aitusoftware.network.patterns.config.Messages;
import com.aitusoftware.network.patterns.measurement.LatencyRecorder;
import com.aitusoftware.network.patterns.measurement.Timer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public final class SingleThreadedRequestClient
{
    private static final long BASE_TIMESTAMP = System.nanoTime();
    private final ReadableByteChannel inputChannel;
    private final WritableByteChannel outputChannel;
    private final ByteBuffer payload;
    private final long warmupMessages;
    private final LatencyRecorder latencyRecorder;
    private final Timer timer;

    SingleThreadedRequestClient(
            final ReadableByteChannel inputChannel, final WritableByteChannel outputChannel,
            final int payloadSize, final LatencyRecorder latencyRecorder,
            final long warmupMessages)
    {
        this.inputChannel = inputChannel;
        this.outputChannel = outputChannel;
        payload = ByteBuffer.allocateDirect(payloadSize);
        this.latencyRecorder = latencyRecorder;
        this.warmupMessages = warmupMessages;
        this.timer = Timer.expiringIn(Constants.RUNTIME_MINUTES, TimeUnit.MINUTES);
    }

    void sendLoop()
    {
        ThreadAffinity.setThreadAffinity(ThreadAffinity.ThreadId.CLIENT_OUTBOUND);
        System.out.printf("Starting request client at %s%n", Instant.now());
        final long histogramClearInterval = warmupMessages / 500;
        long warmUpMessagesRemaining = warmupMessages;
        short sequenceNumber = 0;
        Thread.currentThread().setName(getClass().getSimpleName() + "-sendLoop");
        final long startNanos = System.nanoTime();

        try
        {
            while (!Thread.currentThread().isInterrupted() && timer.isBeforeDeadline())
            {
                warmUpMessagesRemaining =
                        recordSingleMessageLatency(histogramClearInterval, warmUpMessagesRemaining,
                                sequenceNumber, startNanos);
                sequenceNumber++;
            }

        }
        finally
        {
            Io.closeQuietly(outputChannel);
            Io.closeQuietly(inputChannel);
            latencyRecorder.complete();
            System.out.printf("Request workload complete at %s%n", Instant.now());
        }
    }

    private long recordSingleMessageLatency(final long histogramClearInterval, long warmUpMessagesRemaining, final short sequenceNumber, final long startNanos)
    {
        try
        {
            sendMessage(sequenceNumber, startNanos);
            receiveMessage();
            recordLatency(sequenceNumber, startNanos);
            if (warmUpMessagesRemaining != 0)
            {
                warmUpMessagesRemaining--;
                if (warmUpMessagesRemaining % histogramClearInterval == 0 || warmUpMessagesRemaining == 0)
                {
                    latencyRecorder.reset();
                }
            }
        }
        catch (IOException e)
        {
            System.err.printf("Failed to make request: %s. Exiting.%n", e.getMessage());
            e.printStackTrace();
        }
        return warmUpMessagesRemaining;
    }

    private void recordLatency(final int sequenceNumber, final long startNanos)
    {
        if (payload.remaining() != 0)
        {
            latencyRecorder.messagesDropped(1);
        }
        final long response = payload.getLong(0);
        final long receivedTime = Messages.trimmedTimestamp(System.nanoTime(), BASE_TIMESTAMP);
        final short receivedSequence = (short) (response >> 48);
        if (receivedSequence != sequenceNumber)
        {
            latencyRecorder.messagesDropped(sequenceNumber - receivedSequence);
        }
        else
        {
            latencyRecorder.recordValue(receivedTime - Messages.maskTimestamp(response));
        }
    }

    private void receiveMessage() throws IOException
    {
        payload.clear();
        do
        {
            if (inputChannel.read(payload) == -1)
            {
                Thread.currentThread().interrupt();
                return;
            }
        } while (payload.remaining() != 0 && timer.isBeforeDeadline());
    }

    private void sendMessage(final short sequenceNumber, final long startNanos) throws IOException
    {
        payload.clear();
        final long sendingTime = Messages.trimmedTimestamp(System.nanoTime(), BASE_TIMESTAMP);
        Messages.setRequestDataSinglePayload(payload, sendingTime | ((long) sequenceNumber) << 48);
        Io.sendAll(payload, outputChannel);
    }
}