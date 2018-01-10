package com.aitusoftware.network.patterns.measurement;

public interface LatencyRecorder
{
    void recordValue(final long value);
    void reset();
    void complete();
    void messagesDropped(final int messageCount);
}
