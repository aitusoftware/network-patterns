package com.aitusoftware.network.patterns.app;

import com.aitusoftware.network.patterns.config.Connection;
import com.aitusoftware.network.patterns.config.Constants;
import com.aitusoftware.network.patterns.config.HistogramFactory;
import com.aitusoftware.network.patterns.config.Mode;
import com.aitusoftware.network.patterns.measurement.SimpleHistogramRecorder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public final class SingleThreadedDuplexTcpRunner
{
    private final Mode mode;
    private final Connection connection;
    private final InetSocketAddress address;
    private final ExecutorService executor;
    private final int payloadSize;

    SingleThreadedDuplexTcpRunner(
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
                final SocketChannel clientOutput = connectToRemoteAddress(address);
                System.out.printf("Client made outgoing connection to %s%n", clientOutput);
                final InetSocketAddress bindAddress = new InetSocketAddress("0.0.0.0", address.getPort() + 1);
                System.out.printf("Client waiting for incoming connection on %s%n", bindAddress);
                final SocketChannel clientInput = acceptConnection(bindAddress);
                System.out.printf("Client received incoming connection %s%n", clientInput);
                return executor.submit(new SingleThreadedRequestClient(clientInput, clientOutput, payloadSize, latencyRecorder, 500_000, 1_500_000)::sendLoop);

            case SERVER:
                final SocketChannel serverInput = acceptConnection(address);
                System.out.printf("Server received incoming connection %s%n", serverInput);
                final InetAddress remoteClientAddress = serverInput.socket().getInetAddress();
                final InetSocketAddress remoteAddress = new InetSocketAddress(remoteClientAddress, address.getPort() + 1);
                System.out.printf("Server attempting outgoing connection to %s%n", remoteAddress);
                final SocketChannel serverOutput = connectToRemoteAddress(remoteAddress);
                System.out.printf("Server establised outgoing connection %s%n", serverOutput);
                return executor.submit(new SingleThreadedResponseServer(serverInput, serverOutput, payloadSize)::receiveLoop);

            default:
                throw new IllegalArgumentException();
        }
    }

    private SocketChannel acceptConnection(final InetSocketAddress bindAddress)
    {
        try
        {
            final ServerSocketChannel serverSocket = ServerSocketChannel.open();
            serverSocket.bind(bindAddress);
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
                    new IOException("Could not connect to server at " + bindAddress + " within timeout"));

        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Could not bind to address: " + bindAddress, e);
        }
    }

    private SocketChannel connectToRemoteAddress(final InetSocketAddress remoteAddress)
    {
        final long timeoutAt = System.currentTimeMillis() + Constants.CONNECT_TIMEOUT_MILLIS;
        while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < timeoutAt)
        {
            try
            {
                final SocketChannel channel = SocketChannel.open(remoteAddress);
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
                new IOException("Could not connect to server at " + remoteAddress + " within timeout"));
    }
}