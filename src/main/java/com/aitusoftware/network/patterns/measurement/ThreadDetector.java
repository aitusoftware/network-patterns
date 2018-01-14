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
        Thread.getAllStackTraces().keySet().stream().forEach(t -> {
            final String threadId = toId(t);
            if (!threadIds.contains(threadId))
            {
                throw new AssertionError("Found rogue thread: " + t.getName() + "\n" +
                        Arrays.toString(t.getStackTrace()));
            }
        });
    }

    private static String toId(final Thread thread)
    {
        return thread.getId() + thread.getName();
    }

}
