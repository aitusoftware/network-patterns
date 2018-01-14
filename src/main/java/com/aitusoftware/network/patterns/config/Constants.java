package com.aitusoftware.network.patterns.config;

import java.util.concurrent.TimeUnit;

public interface Constants
{
    long CONNECT_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(30L);
    long WARMUP_MESSAGES = Long.getLong("network-patterns.warmupMessages", 500_000);
    long RUNTIME_MINUTES = Long.getLong("network-patterns.runtimeMinutes", 1);
    String BIND_ADDRESS = System.getProperty("network-patterns.bindAddress", "0.0.0.0");
    String CONNECT_ADDRESS = System.getProperty("network-patterns.connectAddress", "127.0.0.1");
    int SERVER_LISTEN_PORT = Integer.getInteger("network-patterns.serverListenPort", 7786);
    int CLIENT_LISTEN_PORT = Integer.getInteger("network-patterns.clientListenPort", 7788);
}
