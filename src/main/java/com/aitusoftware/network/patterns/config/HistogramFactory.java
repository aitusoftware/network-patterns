package com.aitusoftware.network.patterns.config;

import org.HdrHistogram.Histogram;

import java.util.concurrent.TimeUnit;

public final class HistogramFactory
{
    private HistogramFactory() {}

    public static Histogram create()
    {
        return new Histogram(TimeUnit.SECONDS.toNanos(1), 4);
    }
}
