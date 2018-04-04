package com.aitusoftware.network.patterns.measurement;

import com.aitusoftware.network.patterns.config.HistogramFactory;
import org.HdrHistogram.Histogram;

import java.util.function.Consumer;
import java.util.function.LongConsumer;

public final class SimpleHistogramRecorder implements LatencyRecorder
{
    private final Histogram histogram;
    private final Consumer<Histogram> completeListener;
    private final LongConsumer dropListener;
    private final long maxValue;
    private long droppedMessages;

    public SimpleHistogramRecorder(
            final Histogram histogram, final Consumer<Histogram> completeListener,
            final LongConsumer dropListener)
    {
        this.histogram = histogram;
        this.completeListener = completeListener;
        this.dropListener = dropListener;
        this.maxValue = histogram.getHighestTrackableValue();
    }

    public static LatencyRecorder printToStdOut()
    {
        return new SimpleHistogramRecorder(HistogramFactory.create(), h -> {
            System.out.printf("99%% = %d%n", h.getValueAtPercentile(99));
//            h.outputPercentileDistribution(System.out, 1d)
        }, d -> System.out.printf("Dropped %d messages%n", d));
    }

    @Override
    public void recordValue(final long value)
    {
        histogram.recordValue(Math.min(value, maxValue));
    }

    @Override
    public void recordValueWithExpectedInterval(final long value, final long expectedInterval)
    {
        histogram.recordValueWithExpectedInterval(Math.min(value, maxValue), expectedInterval);
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
