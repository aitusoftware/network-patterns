package com.aitusoftware.network.patterns.app;

import com.aitusoftware.network.patterns.config.Connection;
import com.aitusoftware.network.patterns.config.Constants;
import com.aitusoftware.network.patterns.config.HistogramFactory;
import com.aitusoftware.network.patterns.config.Mode;
import com.aitusoftware.network.patterns.config.Protocol;
import com.aitusoftware.network.patterns.config.Threading;
import com.aitusoftware.network.patterns.config.Transport;
import com.aitusoftware.network.patterns.measurement.LatencyRecorder;
import com.aitusoftware.network.patterns.measurement.SimpleHistogramRecorder;
import com.aitusoftware.network.patterns.measurement.ThreadDetector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.time.Instant;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class RunAllServers
{
    public static void main(String[] args) throws Exception
    {
        final Protocol protocol = Protocol.valueOf(args[0]);
        final ThreadDetector detector = new ThreadDetector();
        detector.recordThreads();
        for (Connection connection : Connection.values())
        {
            for (Transport transport : Transport.values())
            {
                for (Threading threading : Threading.values())
                {
                    switch (protocol)
                    {
                        case TCP:
                            runTcpTest(connection, transport, threading);
                            detector.assertNoNewThreads();
                            break;
                        case UDP:
                            runUdpTest(connection, transport, threading);
                            detector.assertNoNewThreads();
                            break;
                        default:
                            throw new IllegalArgumentException();
                    }
                }
            }
        }
    }

    private static void runTcpTest(final Connection connection, final Transport transport, final Threading threading) throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException
    {
        final ExecutorService threadPool = Executors.newCachedThreadPool();
        try
        {
            System.out.printf("TCP %s %s %s at %s%n",
                    transport, threading, connection, Instant.now());
            final LatencyRecorder latencyRecorder =
                    new SimpleHistogramRecorder(HistogramFactory.create(), h -> {
                        try
                        {
                            h.outputPercentileDistribution(new PrintStream(
                                    new File(String.format("TCP_%s_%s_%s.txt", transport, threading, connection))), 1d);
                        }
                        catch (FileNotFoundException e)
                        {
                            e.printStackTrace();
                        }
                    }, d -> {
                    });
            final Future<Future<?>> server = threadPool.submit(() -> TcpServiceFactory.startService(
                    Mode.SERVER, transport, threading, connection, threadPool, latencyRecorder));

            final Future<?> serverStart = server.get(1, TimeUnit.MINUTES);
            Scheduler.delayedCancel(serverStart, Constants.RUNTIME_MINUTES, TimeUnit.MINUTES, threadPool);
            serverStart.get(Constants.RUNTIME_MINUTES + 1, TimeUnit.MINUTES);
        }
        catch (RuntimeException e)
        {
            if (!(e instanceof CancellationException))
            {
                e.printStackTrace();
            }
        }
        finally
        {
            System.out.printf("Shutting down thread pool at %s%n", Instant.now());
            threadPool.shutdown();
            threadPool.shutdownNow();
            if (!threadPool.awaitTermination(15, TimeUnit.SECONDS))
            {
                throw new AssertionError("Thread pool did not shut down");
            }
        }
    }

    private static void runUdpTest(final Connection connection, final Transport transport, final Threading threading) throws InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException
    {
        final ExecutorService threadPool = Executors.newCachedThreadPool();
        try
        {
            System.out.printf("UDP %s %s %s at %s%n",
                    transport, threading, connection, Instant.now());
            final LatencyRecorder latencyRecorder =
                    new SimpleHistogramRecorder(HistogramFactory.create(), h -> {
                        try
                        {
                            h.outputPercentileDistribution(new PrintStream(
                                    new File(String.format("UDP_%s_%s_%s.txt", transport, threading, connection))), 1d);
                        }
                        catch (FileNotFoundException e)
                        {
                            e.printStackTrace();
                        }
                    }, d -> {
                    });
            final Future<Future<?>> server = threadPool.submit(() -> UdpServiceFactory.startService(
                    Mode.SERVER, transport, threading, connection, threadPool, latencyRecorder));

            final Future<?> serverStart = server.get(1, TimeUnit.MINUTES);
            Scheduler.delayedCancel(serverStart, Constants.RUNTIME_MINUTES, TimeUnit.MINUTES, threadPool);
            serverStart.get(Constants.RUNTIME_MINUTES + 1, TimeUnit.MINUTES);
        }
        catch (RuntimeException e)
        {
            if (!(e instanceof CancellationException))
            {
                e.printStackTrace();
            }
        }
        finally
        {
            threadPool.shutdown();
            threadPool.shutdownNow();
            if (!threadPool.awaitTermination(15, TimeUnit.SECONDS))
            {
                throw new AssertionError("Thread pool did not shut down");
            }
        }
    }
}
