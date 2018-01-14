package com.aitusoftware.network.patterns.config;

import java.util.concurrent.TimeUnit;

public interface Constants
{
    long CONNECT_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(30L);
    long WARMUP_MESSAGES = Long.getLong("network-patterns.warmupMessages", 500_000);
    long MEASUREMENT_MESSAGES = Long.getLong("network-patterns.measurementMessages", 1_500_000);
    String BIND_ADDRESS = System.getProperty("network-patterns.bindAddress", "0.0.0.0");
}
