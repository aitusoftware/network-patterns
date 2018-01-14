package com.aitusoftware.network.patterns.app;

import com.aitusoftware.network.patterns.config.Connection;
import com.aitusoftware.network.patterns.config.Constants;
import com.aitusoftware.network.patterns.config.Mode;
import com.aitusoftware.network.patterns.config.Threading;
import com.aitusoftware.network.patterns.measurement.LatencyRecorder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public final class DuplexUdpRunner
{
    private final Mode mode;
    private final Connection connection;
    private final Threading threading;
    private final InetSocketAddress address;
    private final ExecutorService executor;
    private final int payloadSize;

    DuplexUdpRunner(
            final Mode mode, final Connection connection,
            final Threading threading, final InetSocketAddress address, final ExecutorService executor,
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
                final DatagramChannel clientOutput = connectToRemoteAddress(address);
                return startClient(latencyRecorder, clientOutput);

            case SERVER:
                final DatagramChannel serverInput = acceptConnection(address);
                return startServer(serverInput);

            default:
                throw new IllegalArgumentException();
        }
    }

    private Future<?> startServer(final DatagramChannel serverInput)
    {
        final InetAddress remoteClientAddress = serverInput.socket().getInetAddress();
        final InetSocketAddress remoteAddress = new InetSocketAddress(remoteClientAddress, address.getPort() + 1);
        final DatagramChannel serverOutput = connectToRemoteAddress(remoteAddress);

        switch (threading)
        {
            case SINGLE_THREADED:
                return executor.submit(new SingleThreadedResponseServer(serverInput, serverOutput, payloadSize)::receiveLoop);
            case MULTI_THREADED:
                final MultiThreadedResponseServer server = new MultiThreadedResponseServer(serverInput, serverOutput, payloadSize);
                executor.submit(server::responseLoop);
                return executor.submit(server::receiveLoop);
            default:
                throw new IllegalArgumentException();

        }
    }

    private Future<?> startClient(final LatencyRecorder latencyRecorder, final DatagramChannel clientOutput)
    {
        // TODO UDP/TCP interface bind config for all modes
        final InetSocketAddress bindAddress = new InetSocketAddress("0.0.0.0", address.getPort() + 1);
        final DatagramChannel clientInput = acceptConnection(bindAddress);

        switch (threading)
        {
            case SINGLE_THREADED:
                return executor.submit(new SingleThreadedRequestClient(clientInput, clientOutput, payloadSize, latencyRecorder, Constants.WARMUP_MESSAGES, Constants.MEASUREMENT_MESSAGES)::sendLoop);
            case MULTI_THREADED:
                final MultiThreadedRequestClient client = new MultiThreadedRequestClient(clientInput, clientOutput, payloadSize, latencyRecorder, Constants.WARMUP_MESSAGES, Constants.MEASUREMENT_MESSAGES);
                executor.submit(client::receiveLoop);
                return executor.submit(client::sendLoop);
            default:
                throw new IllegalArgumentException();
        }
    }

    private DatagramChannel acceptConnection(final InetSocketAddress bindAddress)
    {
        final UdpHandshake handshake = new UdpHandshake();
        final long timeoutAt = System.currentTimeMillis() + Constants.CONNECT_TIMEOUT_MILLIS;
        while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < timeoutAt)
        {
            try
            {
                final DatagramChannel channel = DatagramChannel.open();
                channel.bind(bindAddress);
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
                new IOException("Could not connect to server at " + bindAddress + " within timeout"));
    }

    private DatagramChannel connectToRemoteAddress(final InetSocketAddress remoteAddress)
    {
        final long timeoutAt = System.currentTimeMillis() + Constants.CONNECT_TIMEOUT_MILLIS;
        final UdpHandshake handshake = new UdpHandshake();
        while (!Thread.currentThread().isInterrupted() && System.currentTimeMillis() < timeoutAt)
        {
            try
            {
                final DatagramChannel channel = handshake.initiateSend(remoteAddress);
                channel.configureBlocking(connection.isBlocking());
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