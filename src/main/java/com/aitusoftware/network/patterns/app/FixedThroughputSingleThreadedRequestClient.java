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

public final class FixedThroughputSingleThreadedRequestClient
{
    private static final long BASE_TIMESTAMP = System.nanoTime();
    private final ReadableByteChannel inputChannel;
    private final WritableByteChannel outputChannel;
    private final ByteBuffer payloadOut;
    private final ByteBuffer payloadIn;
    private final long warmupMessages;
    private final LatencyRecorder latencyRecorder;
    private final Timer timer;
    private final long spinDelayBetweenMessages =
            (TimeUnit.SECONDS.toNanos(1L) / Constants.THROUGHPUT) - 20L;
    private long nextSendingTimeNanos = 0L;
    private short expectedSequenceNumber = 0;
    private short nextSequenceNumber = 0;
    private boolean messageReceived = false;
    private long sent;

    FixedThroughputSingleThreadedRequestClient(
            final ReadableByteChannel inputChannel, final WritableByteChannel outputChannel,
            final int payloadSize, final LatencyRecorder latencyRecorder,
            final long warmupMessages)
    {
        this.inputChannel = inputChannel;
        this.outputChannel = outputChannel;
        payloadOut = ByteBuffer.allocateDirect(payloadSize);
        payloadIn = ByteBuffer.allocateDirect(payloadSize);
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
        final long histogramClearInterval = warmupMessages / 500;
        long warmUpMessagesRemaining = warmupMessages;
        Thread.currentThread().setName(getClass().getSimpleName() + "-sendLoop");

        try
        {
            nextSendingTimeNanos = System.nanoTime();
            while (!Thread.currentThread().isInterrupted() && sent < 10_000_000)
            {
                warmUpMessagesRemaining =
                        recordSingleMessageLatency(histogramClearInterval, warmUpMessagesRemaining);
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

    private long recordSingleMessageLatency(
            final long histogramClearInterval, long warmUpMessagesRemaining)
    {
        try
        {
            messageReceived = false;
            sendMessage();
            receiveMessage();
            recordLatency();
            if (warmUpMessagesRemaining != 0)
            {
                if (messageReceived)
                {
                    warmUpMessagesRemaining--;
                    if (warmUpMessagesRemaining % histogramClearInterval == 0 || warmUpMessagesRemaining == 0)
                    {
                        latencyRecorder.reset();
                    }
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

    private void recordLatency()
    {
        if (payloadIn.remaining() == 0)
        {
            final long response = payloadIn.getLong(0);
            final long receivedTime = Messages.trimmedTimestamp(System.nanoTime(), BASE_TIMESTAMP);
            final short receivedSequence = (short) (response >> 48);

            if (receivedSequence != expectedSequenceNumber)
            {
                latencyRecorder.messagesDropped(receivedSequence - expectedSequenceNumber);
            }
            else
            {
                latencyRecorder.recordValueWithExpectedInterval(
                        receivedTime - Messages.maskTimestamp(response), spinDelayBetweenMessages);
                expectedSequenceNumber++;
            }
            payloadIn.clear();
            messageReceived = true;
        }
    }

    private void receiveMessage() throws IOException
    {
        if (inputChannel.read(payloadIn) == -1)
        {
            Thread.currentThread().interrupt();
        }
    }

    private void sendMessage() throws IOException
    {
        if (System.nanoTime() >= nextSendingTimeNanos)
        {
            payloadOut.clear();
            final long sendingTime = Messages.trimmedTimestamp(System.nanoTime(), BASE_TIMESTAMP);
            Messages.setRequestDataSinglePayload(payloadOut, sendingTime | ((long) nextSequenceNumber) << 48);
            Io.sendAll(payloadOut, outputChannel);
            nextSendingTimeNanos = nextSendingTimeNanos + spinDelayBetweenMessages;
            nextSequenceNumber++;
            sent++;
        }
    }
}