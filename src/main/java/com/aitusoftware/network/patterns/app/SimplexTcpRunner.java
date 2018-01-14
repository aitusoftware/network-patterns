package com.aitusoftware.network.patterns.app;

import com.aitusoftware.network.patterns.config.Connection;
import com.aitusoftware.network.patterns.config.Constants;
import com.aitusoftware.network.patterns.config.Mode;
import com.aitusoftware.network.patterns.config.Threading;
import com.aitusoftware.network.patterns.measurement.LatencyRecorder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public final class SimplexTcpRunner
{
    private final Mode mode;
    private final Connection connection;
    private final Threading threading;
    private final InetSocketAddress address;
    private final ExecutorService executor;
    private final int payloadSize;

    SimplexTcpRunner(
            final Mode mode, final Connection connection,
            final Threading threading, final InetSocketAddress address,
            final ExecutorService executor,
            final int payloadSize)
    {

        this.mode = mode;
        this.connection = connection;
        this.threading = threading;
        this.address = address;
        this.executor = executor;
        this.payloadSize = payloadSize;
    }

    Future<?> start(final LatencyRecorder latencyRecorder)
    {
        switch (mode)
        {
            case CLIENT:
                final SocketChannel clientChannel = connectToRemoteAddress();
                return startClient(latencyRecorder, clientChannel);

            case SERVER:
                final SocketChannel serverChannel = acceptConnection();
                return startServer(serverChannel);

            default:
                throw new IllegalArgumentException();
        }
    }

    private Future<?> startServer(final SocketChannel serverChannel)
    {
        switch (threading)
        {
            case SINGLE_THREADED:
                return executor.submit(new SingleThreadedResponseServer(serverChannel, serverChannel, payloadSize)::receiveLoop);
            case MULTI_THREADED:
                final MultiThreadedResponseServer server = new MultiThreadedResponseServer(serverChannel, serverChannel, payloadSize);
                executor.submit(server::responseLoop);
                return executor.submit(server::receiveLoop);
            default:
                throw new IllegalArgumentException();
        }
    }

    private Future<?> startClient(final LatencyRecorder latencyRecorder, final SocketChannel clientChannel)
    {
        switch (threading)
        {
            case SINGLE_THREADED:
                return executor.submit(new SingleThreadedRequestClient(clientChannel, clientChannel, payloadSize, latencyRecorder, Constants.WARMUP_MESSAGES, Constants.MEASUREMENT_MESSAGES)::sendLoop);
            case MULTI_THREADED:

                final MultiThreadedRequestClient client = new MultiThreadedRequestClient(clientChannel, clientChannel, payloadSize, latencyRecorder, Constants.WARMUP_MESSAGES, Constants.MEASUREMENT_MESSAGES);
                executor.submit(client::receiveLoop);
                return executor.submit(client::sendLoop);
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
            serverSocket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            serverSocket.setOption(StandardSocketOptions.SO_REUSEPORT, true);
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
                    Io.closeQuietly(serverSocket);
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