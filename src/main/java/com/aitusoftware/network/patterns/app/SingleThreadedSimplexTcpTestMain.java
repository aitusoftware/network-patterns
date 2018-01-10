package com.aitusoftware.network.patterns.app;

import com.aitusoftware.network.patterns.config.Connection;
import com.aitusoftware.network.patterns.config.Mode;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class SingleThreadedSimplexTcpTestMain
{
    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException
    {
        final Mode mode = Mode.valueOf(args[0]);
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 7786);

        final ExecutorService pool = Executors.newFixedThreadPool(2);
        final SingleThreadedSimplexTcpRunner runner = new SingleThreadedSimplexTcpRunner(
                mode, Connection.BLOCKING, address, pool, 256);

        final Future<?> task = runner.start();

        task.get(20, TimeUnit.MINUTES);
        pool.shutdownNow();
    }
}
