package com.aitusoftware.network.patterns.app;

import com.aitusoftware.network.patterns.config.Connection;
import com.aitusoftware.network.patterns.config.Constants;
import com.aitusoftware.network.patterns.config.Mode;
import com.aitusoftware.network.patterns.measurement.LatencyRecorder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public final class SingleThreadedSimplexUdpRunner
{
    private final Mode mode;
    private final Connection connection;
    private final InetSocketAddress address;
    private final ExecutorService executor;
    private final int payloadSize;

    SingleThreadedSimplexUdpRunner(
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

    Future<?> start(final LatencyRecorder latencyRecorder)
    {
        switch (mode)
        {
            case CLIENT:
                final DatagramChannel clientChannel = connectToRemoteAddress();
                return executor.submit(new SingleThreadedRequestClient(clientChannel, clientChannel, payloadSize, latencyRecorder, 500_000, 1_500_000)::sendLoop);

            case SERVER:
                final DatagramChannel serverChannel = acceptConnection();
                return executor.submit(new SingleThreadedResponseServer(serverChannel, serverChannel, payloadSize)::receiveLoop);

            default:
                throw new IllegalArgumentException();
        }
    }

    private DatagramChannel acceptConnection()
    {
        final UdpHandshake handshake = new UdpHandshake();
        final long timeoutAt = System.currentTimeMillis() + Constants.CONNECT_TIMEOUT_MILLIS;
        while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < timeoutAt)
        {
            try
            {
                final DatagramChannel channel = DatagramChannel.open();
                channel.bind(address);
                channel.configureBlocking(connection.isBlocking());
                handshake.performReceive(channel);

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

    private DatagramChannel connectToRemoteAddress()
    {
        final long timeoutAt = System.currentTimeMillis() + Constants.CONNECT_TIMEOUT_MILLIS;
        final UdpHandshake handshake = new UdpHandshake();
        while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < timeoutAt)
        {
            try
            {
                final DatagramChannel channel = handshake.initiateSend(address);
                channel.configureBlocking(connection.isBlocking());
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