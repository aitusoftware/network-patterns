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

public final class FixedThroughputMultiThreadedRequestClient
{
    private static final long BASE_TIMESTAMP = System.nanoTime();
    private final ReadableByteChannel inputChannel;
    private final WritableByteChannel outputChannel;
    private final ByteBuffer requestBuffer;
    private final ByteBuffer responseBuffer;
    private final long warmupMessages;
    private final LatencyRecorder latencyRecorder;
    private final Timer timer;
    private final long spinDelayBetweenMessages =
            (TimeUnit.SECONDS.toNanos(1L) / Constants.THROUGHPUT) - 20L;
    private long nextSendingTimeNanos = 0L;
    private short expectedSequenceNumber = 0;
    private short nextSequenceNumber = 0;

    FixedThroughputMultiThreadedRequestClient(
            final ReadableByteChannel inputChannel, final WritableByteChannel outputChannel,
            final int payloadSize, final LatencyRecorder latencyRecorder,
            final long warmupMessages)
    {
        this.inputChannel = inputChannel;
        this.outputChannel = outputChannel;
        requestBuffer = ByteBuffer.allocateDirect(payloadSize);
        responseBuffer = ByteBuffer.allocateDirect(payloadSize);
        this.latencyRecorder = latencyRecorder;
        this.warmupMessages = warmupMessages;
        this.timer = Timer.expiringIn(Constants.RUNTIME_MINUTES, TimeUnit.MINUTES);
    }

    void sendLoop()
    {
        ThreadAffinity.setThreadAffinity(ThreadAffinity.ThreadId.CLIENT_OUTBOUND);
        System.out.printf("Starting request client at %s%n", Instant.now());
        System.out.printf("Target throughput: %d, inter-message delay: %dns%n",
                Constants.THROUGHPUT, spinDelayBetweenMessages);
        Thread.currentThread().setName(getClass().getSimpleName() + "-sendLoop");

        try
        {
            nextSendingTimeNanos = System.nanoTime();
            while (!Thread.currentThread().isInterrupted() && timer.isBeforeDeadline())
            {
                try
                {
                    sendMessage();
                }
                catch (IOException e)
                {
                    System.err.printf("Failed to make request: %s. Exiting.%n", e.getMessage());
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                    closeConnections();
                }
            }
        }
        finally
        {
            closeConnections();
            System.out.printf("Request workload complete at %s%n", Instant.now());
            latencyRecorder.complete();
        }
    }

    void receiveLoop()
    {
        ThreadAffinity.setThreadAffinity(ThreadAffinity.ThreadId.CLIENT_INBOUND);
        Thread.currentThread().setName(getClass().getSimpleName() + "-receiveLoop");
        final long histogramClearInterval = warmupMessages / 500;
        long warmUpMessagesRemaining = warmupMessages;
        while (!Thread.currentThread().isInterrupted() && timer.isBeforeDeadline())
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
                    recordLatency();
                    if (warmUpMessagesRemaining != 0)
                    {
                        warmUpMessagesRemaining--;
                        if (warmUpMessagesRemaining % histogramClearInterval == 0
                                || warmUpMessagesRemaining == 0)
                        {
                            latencyRecorder.reset();
                        }
                    }

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

    private void recordLatency()
    {
        final long response = responseBuffer.getLong(0);

        final long receivedTime = Messages.trimmedTimestamp(System.nanoTime(), BASE_TIMESTAMP);
        final short receivedSequence = (short) (response >> 48);

        if (receivedSequence != expectedSequenceNumber)
        {
            latencyRecorder.messagesDropped(receivedSequence - expectedSequenceNumber);
        }
        else
        {
            final long latencyNanos = receivedTime - Messages.maskTimestamp(response);
            latencyRecorder.recordValueWithExpectedInterval(latencyNanos, spinDelayBetweenMessages);
            expectedSequenceNumber++;
        }
    }

    private void closeConnections()
    {
        Io.closeQuietly(inputChannel);
        Io.closeQuietly(outputChannel);
    }

    private void sendMessage() throws IOException
    {
        if (System.nanoTime() >= nextSendingTimeNanos)
        {
            requestBuffer.clear();
            final long sendingTime = Messages.trimmedTimestamp(System.nanoTime(), BASE_TIMESTAMP);

            Messages.setRequestDataSinglePayload(requestBuffer, sendingTime | ((long) nextSequenceNumber) << 48);
            Io.sendAll(requestBuffer, outputChannel);
            nextSendingTimeNanos = nextSendingTimeNanos + spinDelayBetweenMessages;
            nextSequenceNumber++;
        }
    }
}