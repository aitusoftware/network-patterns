package com.aitusoftware.network.patterns.measurement;

import org.HdrHistogram.Histogram;

import java.util.function.Consumer;
import java.util.function.LongConsumer;

public final class SimpleHistogramRecorder implements LatencyRecorder
{
    private final Histogram histogram;
    private final Consumer<Histogram> completeListener;
    private final LongConsumer dropListener;
    private long droppedMessages;

    public SimpleHistogramRecorder(
            final Histogram histogram, final Consumer<Histogram> completeListener,
            final LongConsumer dropListener)
    {
        this.histogram = histogram;
        this.completeListener = completeListener;
        this.dropListener = dropListener;
    }

    @Override
    public void recordValue(final long value)
    {
        histogram.recordValue(value);
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
