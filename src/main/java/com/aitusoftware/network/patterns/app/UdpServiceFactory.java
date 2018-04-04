package com.aitusoftware.network.patterns.app;

import com.aitusoftware.network.patterns.config.Connection;
import com.aitusoftware.network.patterns.config.Constants;
import com.aitusoftware.network.patterns.config.Mode;
import com.aitusoftware.network.patterns.config.Threading;
import com.aitusoftware.network.patterns.config.Transport;
import com.aitusoftware.network.patterns.measurement.LatencyRecorder;
import com.aitusoftware.network.patterns.measurement.SimpleHistogramRecorder;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class UdpServiceFactory
{
    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException
    {
        final Mode mode = Mode.valueOf(args[0]);
        final Transport transport = Transport.valueOf(args[1]);
        final Connection connection = Connection.valueOf(args[2]);

        final ExecutorService pool = Executors.newCachedThreadPool();
        final LatencyRecorder latencyRecorder = SimpleHistogramRecorder.printToStdOut();
        final Future<?> task = startService(mode, Transport.SIMPLEX, Threading.SINGLE_THREADED, Connection.BLOCKING, pool, latencyRecorder);

        task.get(20, TimeUnit.MINUTES);
        pool.shutdownNow();
    }

    static Future<?> startService(final Mode mode, final Transport transport, final Threading threading,
                                  final Connection connection, final ExecutorService pool,
                                  final LatencyRecorder latencyRecorder)
    {
        final InetSocketAddress address = new InetSocketAddress(Constants.SERVER_BIND_ADDRESS, Constants.SERVER_LISTEN_PORT);
        return runTask(mode, address, pool, transport, connection, threading, latencyRecorder);
    }

    private static Future<?> runTask(
            final Mode mode, final InetSocketAddress address,
            final ExecutorService pool, final Transport transport, final Connection connection, final Threading threading, final LatencyRecorder latencyRecorder)
    {
        if (mode == Mode.SERVER)
        {
            switch (transport)
            {
                case SIMPLEX:
                    final SimplexUdpRunner simplex = new SimplexUdpRunner(
                            mode, Connection.NON_BLOCKING, Threading.SINGLE_THREADED, address, pool, 256);

                    return simplex.start(latencyRecorder);
                case DUPLEX:
                    final DuplexUdpRunner duplex = new DuplexUdpRunner(
                            mode, Connection.NON_BLOCKING, Threading.SINGLE_THREADED, address, pool, 256);

                    return duplex.start(latencyRecorder);
                default:
                    throw new IllegalArgumentException();
            }
        }


        switch (transport)
        {
            case SIMPLEX:
                final SimplexUdpRunner simplex = new SimplexUdpRunner(
                        mode, connection, threading, address, pool, 256);

                return simplex.start(latencyRecorder);
            case DUPLEX:
                final DuplexUdpRunner duplex = new DuplexUdpRunner(
                        mode, connection, threading, address, pool, 256);

                return duplex.start(latencyRecorder);
            default:
                throw new IllegalArgumentException();
        }
    }

}
