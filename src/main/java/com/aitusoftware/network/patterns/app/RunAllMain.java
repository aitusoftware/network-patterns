package com.aitusoftware.network.patterns.app;

import com.aitusoftware.network.patterns.config.Connection;
import com.aitusoftware.network.patterns.config.HistogramFactory;
import com.aitusoftware.network.patterns.config.Mode;
import com.aitusoftware.network.patterns.config.Threading;
import com.aitusoftware.network.patterns.config.Transport;
import com.aitusoftware.network.patterns.measurement.LatencyRecorder;
import com.aitusoftware.network.patterns.measurement.SimpleHistogramRecorder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class RunAllMain
{
    public static void main(String[] args) throws Exception
    {
        for (Connection connection : Connection.values())
        {
            for (Transport transport : Transport.values())
                {
                    for (Threading threading : Threading.values())
                    {
                        final ExecutorService threadPool = Executors.newCachedThreadPool();
                        try
                        {
                            System.out.printf("TCP %s %s %s%n",
                                    transport, threading, connection);
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
                                    }, d -> {});
                            final Future<Future<?>> server = threadPool.submit(() -> SingleThreadedTcpTestMain.startService(
                                    Mode.SERVER, transport, threading, connection, threadPool, latencyRecorder));

                            final Future<Future<?>> client = threadPool.submit(() -> SingleThreadedTcpTestMain.startService(
                                    Mode.CLIENT, transport, threading, connection, threadPool, latencyRecorder));

                            final Future<?> clientStart = client.get(1, TimeUnit.MINUTES);
                            final Future<?> serverStart = server.get(1, TimeUnit.MINUTES);
                            clientStart.get(1, TimeUnit.MINUTES);
                            serverStart.get(1, TimeUnit.MINUTES);
                        }
                        finally
                        {
                            threadPool.shutdownNow();
                        }
                    }
                }
        }
    }
}
