package com.aitusoftware.network.patterns.measurement;

public interface LatencyRecorder
{
    void recordValue(final long value);
    void recordValueWithExpectedInterval(final long value, final long expectedInterval);
    void reset();
    void complete();
    void messagesDropped(final long messageCount);
}
