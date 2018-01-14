package com.aitusoftware.network.patterns.measurement;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ThreadDetector
{
    private final Set<String> threadIds = new HashSet<>();

    public void recordThreads()
    {
        Thread.getAllStackTraces().keySet().stream().map(ThreadDetector::toId).forEach(threadIds::add);
    }

    public void assertNoNewThreads()
    {
        Thread.getAllStackTraces().keySet().forEach(t -> {
            final String threadId = toId(t);
            if (!threadIds.contains(threadId) && t.isAlive())
            {
                final long terminationTimeout = System.currentTimeMillis() + 5_000L;
                while (t.isAlive() && System.currentTimeMillis() < terminationTimeout)
                {
                    if (!t.isAlive())
                    {
                        return;
                    }
                }
                throw new AssertionError("Found rogue thread: " + t.getName() + " in state: " + t.getState() + "\n" +
                        Arrays.stream(t.getStackTrace()).map(e -> e.toString() + "\n").reduce("", (a, b) -> a + b));
            }
        });
    }

    private static String toId(final Thread thread)
    {
        return thread.getId() + thread.getName();
    }

}
