package com.aitusoftware.network.patterns.app;

public final class ThreadAffinity
{
    enum ThreadId
    {
        CLIENT_OUTBOUND("clientOutbound"),
        CLIENT_INBOUND("clientInbound"),
        SERVER_INBOUND("serverInbound"),
        SERVER_OUTBOUND("serverOutbound");

        private final String propertyKey;

        ThreadId(final String propertyKey)
        {
            this.propertyKey = propertyKey;
        }
    }

    private static final String SYSTEM_PROPERTY_FORMAT = "network-patterns.affinity.%s";
    private static final int NO_AFFINITY = -1;

    static void setThreadAffinity(final ThreadId threadId)
    {
        final int specifiedAffinity = Integer.getInteger(toSystemProperty(threadId), NO_AFFINITY);
        if (specifiedAffinity == NO_AFFINITY)
        {
            return;
        }

        System.out.printf("Setting affinity for thread %s/%s to %d%n",
                threadId.name(), Thread.currentThread().getName(), specifiedAffinity);
    }

    private static String toSystemProperty(final ThreadId threadId)
    {
        return String.format(SYSTEM_PROPERTY_FORMAT, threadId.propertyKey);
    }
}