package com.aitusoftware.network.patterns.app;

import com.aitusoftware.network.patterns.config.Connection;
import com.aitusoftware.network.patterns.config.Mode;
import com.aitusoftware.network.patterns.config.Transport;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class SingleThreadedUdpTestMain
{
    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException
    {
        final Mode mode = Mode.valueOf(args[0]);
        final Transport transport = Transport.valueOf(args[1]);
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 15676);

        final ExecutorService pool = Executors.newFixedThreadPool(2);
        final Future<?> task = runTask(mode, address, pool, transport);

        task.get(20, TimeUnit.MINUTES);
        pool.shutdownNow();
    }

    private static Future<?> runTask(
            final Mode mode, final InetSocketAddress address,
            final ExecutorService pool, final Transport transport)
    {
        switch (transport)
        {
            case SIMPLEX:
                final SingleThreadedSimplexUdpRunner simplex = new SingleThreadedSimplexUdpRunner(
                        mode, Connection.NON_BLOCKING, address, pool, 256);

                return simplex.start();
            case DUPLEX:
                final SingleThreadedDuplexUdpRunner duplex = new SingleThreadedDuplexUdpRunner(
                        mode, Connection.NON_BLOCKING, address, pool, 256);

                return duplex.start();
            default:
                throw new IllegalArgumentException();
        }
    }

}
