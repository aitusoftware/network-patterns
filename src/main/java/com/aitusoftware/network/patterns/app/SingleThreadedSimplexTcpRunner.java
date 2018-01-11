package com.aitusoftware.network.patterns.app;

import com.aitusoftware.network.patterns.config.Connection;
import com.aitusoftware.network.patterns.config.Constants;
import com.aitusoftware.network.patterns.config.HistogramFactory;
import com.aitusoftware.network.patterns.config.Mode;
import com.aitusoftware.network.patterns.measurement.SimpleHistogramRecorder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public final class SingleThreadedSimplexTcpRunner
{

    private final Mode mode;
    private final Connection connection;
    private final InetSocketAddress address;
    private final ExecutorService executor;
    private final int payloadSize;

    SingleThreadedSimplexTcpRunner(
            final Mode mode, final Connection connection,
            final InetSocketAddress address, final ExecutorService executor,
            final int payloadSize)
    {

        this.mode = mode;
        this.connection = connection;
        this.address = address;
        this.executor = executor;
        this.payloadSize = payloadSize;
    }

    Future<?> start()
    {
        final SimpleHistogramRecorder latencyRecorder =
                new SimpleHistogramRecorder(HistogramFactory.create(), h -> {
                    h.outputPercentileDistribution(System.out, 1d);
                }, d -> System.out.printf("Dropped %d messages%n", d));
        switch (mode)
        {
            case CLIENT:
                final SocketChannel clientChannel = connectToRemoteAddress();
                return executor.submit(new SingleThreadedRequestClient(clientChannel, clientChannel, payloadSize, latencyRecorder, 500_000, 500_000)::sendLoop);

            case SERVER:
                final SocketChannel serverChannel = acceptConnection();
                return executor.submit(new SingleThreadedResponseServer(serverChannel, serverChannel, payloadSize)::receiveLoop);

            default:
                throw new IllegalArgumentException();
        }
    }

    private SocketChannel acceptConnection()
    {
        try
        {
            final ServerSocketChannel serverSocket = ServerSocketChannel.open();
            serverSocket.bind(address);
            serverSocket.configureBlocking(true);

            final long timeoutAt = System.currentTimeMillis() + Constants.CONNECT_TIMEOUT_MILLIS;
            while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < timeoutAt)
            {
                try
                {
                    final SocketChannel channel = serverSocket.accept();
                    channel.configureBlocking(connection.isBlocking());
                    while (!channel.finishConnect())
                    {
                        Thread.yield();
                    }
                    return channel;
                }
                catch (IOException e)
                {
                    // server not ready
                }
            }

            throw new UncheckedIOException(
                    new IOException("Could not connect to server at " + address + " within timeout"));

        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Could not bind to address: " + address, e);
        }
    }

    private SocketChannel connectToRemoteAddress()
    {
        final long timeoutAt = System.currentTimeMillis() + Constants.CONNECT_TIMEOUT_MILLIS;
        while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < timeoutAt)
        {
            try
            {
                final SocketChannel channel = SocketChannel.open(address);
                channel.configureBlocking(connection.isBlocking());
                while (!channel.finishConnect())
                {
                    Thread.yield();
                }
                return channel;
            }
            catch (IOException e)
            {
                // server not ready
            }
        }
        throw new UncheckedIOException(
                new IOException("Could not connect to server at " + address + " within timeout"));
    }
}