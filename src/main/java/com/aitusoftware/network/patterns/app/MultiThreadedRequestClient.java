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
import java.util.concurrent.atomic.AtomicBoolean;

public final class MultiThreadedRequestClient
{
    private static final long BASE_TIMESTAMP = System.nanoTime();
    private final ReadableByteChannel inputChannel;
    private final WritableByteChannel outputChannel;
    private final ByteBuffer requestBuffer;
    private final ByteBuffer responseBuffer;
    private final long warmupMessages;
    private final LatencyRecorder latencyRecorder;
    private final Timer timer;
    private final AtomicBoolean sendFlag = new AtomicBoolean(true);
    private long sent;

    MultiThreadedRequestClient(
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
        short sequenceNumber = 0;
        Thread.currentThread().setName(getClass().getSimpleName() + "-sendLoop");

        try
        {
            while (!Thread.currentThread().isInterrupted() && sent < 10_000_000)
            {
                try
                {
                    if (sendFlag.compareAndSet(true, false))
                    {
                        sendMessage(sequenceNumber);
                        sequenceNumber++;
                        sent++;
                    }
                }
                catch (IOException e)
                {
                    System.err.printf("Failed to make request: %s. Exiting.%n", e.getMessage());
                    e.printStackTrace();
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

    private long received = 0;

    void receiveLoop()
    {
        ThreadAffinity.setThreadAffinity(ThreadAffinity.ThreadId.CLIENT_INBOUND);
        Thread.currentThread().setName(getClass().getSimpleName() + "-receiveLoop");
        final long histogramClearInterval = warmupMessages / 500;
        long warmUpMessagesRemaining = warmupMessages;
        short expectedSequenceNumber = 0;
        while (!Thread.currentThread().isInterrupted() && received < 10_000_000)
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
                    final long data = responseBuffer.getLong(0);

                    responseBuffer.clear();
                    recordLatency(expectedSequenceNumber, data);
                    expectedSequenceNumber++;
                    if (warmUpMessagesRemaining != 0)
                    {
                        warmUpMessagesRemaining--;
                        if (warmUpMessagesRemaining % histogramClearInterval == 0
                                || warmUpMessagesRemaining == 0)
                        {
                            latencyRecorder.reset();
                        }
                    }
                    sendFlag.lazySet(true);
                    received++;
                }
            }
            catch (IOException e)
            {
                closeConnections();
                return;
            }
        }
    }

    private void recordLatency(final short sequenceNumber, final long response)
    {
        final long receivedTime = Messages.trimmedTimestamp(System.nanoTime(), BASE_TIMESTAMP);
        final short receivedSequence = (short) (response >> 48);

        if (receivedSequence != sequenceNumber)
        {
            latencyRecorder.messagesDropped(sequenceNumber - receivedSequence);
        }
        else
        {
            final long latencyNanos = receivedTime - Messages.maskTimestamp(response);
            latencyRecorder.recordValue(latencyNanos);
        }
    }

    private void closeConnections()
    {
        Io.closeQuietly(inputChannel);
        Io.closeQuietly(outputChannel);
    }

    private void sendMessage(final short sequenceNumber) throws IOException
    {
        requestBuffer.clear();
        final long sendingTime = Messages.trimmedTimestamp(System.nanoTime(), BASE_TIMESTAMP);

        Messages.setRequestDataSinglePayload(requestBuffer, sendingTime | ((long) sequenceNumber) << 48);
        Io.sendAll(requestBuffer, outputChannel);
    }
}