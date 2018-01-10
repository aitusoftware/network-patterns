package com.aitusoftware.network.patterns.measurement;

import org.HdrHistogram.Histogram;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

public final class IntervalHistogramRecorder implements LatencyRecorder
{
    private final Histogram histogram;
    private final Consumer<Histogram> completeListener;
    private final LongConsumer dropListener;
    private final long expectedIntervalNanos;
    private long droppedMessages;

    IntervalHistogramRecorder(
            final Histogram histogram, final Consumer<Histogram> completeListener,
            final LongConsumer dropListener, final long messagesPerSecond)
    {
        this.histogram = histogram;
        this.completeListener = completeListener;
        this.dropListener = dropListener;
        this.expectedIntervalNanos = TimeUnit.SECONDS.toNanos(1L) / messagesPerSecond;
    }

    @Override
    public void recordValue(final long value)
    {
        histogram.recordValueWithExpectedInterval(value, expectedIntervalNanos);
    }

    @Override
    public void reset()
    {
        histogram.reset();
    }

    @Override
    public void complete()
    {
        completeListener.accept(histogram);
        dropListener.accept(droppedMessages);
    }

    @Override
    public void messagesDropped(final long messageCount)
    {
        droppedMessages += messageCount;
    }
}
