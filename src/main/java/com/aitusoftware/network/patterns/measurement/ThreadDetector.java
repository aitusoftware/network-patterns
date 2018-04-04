package com.aitusoftware.network.patterns.measurement;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

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
            if (!threadIds.contains(threadId) && isStillActive(t) && !t.getName().contains("Attach Listener"))
            {
                final long terminationTimeout = System.currentTimeMillis() + 10_000L;
                while (isStillActive(t) && System.currentTimeMillis() < terminationTimeout)
                {
                    if (!isStillActive(t))
                    {
                        return;
                    }
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1L));
                }
                if (t.getState() == Thread.State.TERMINATED)
                {
                    return;
                }
                throw new AssertionError("Found rogue thread: " + t.getName() + " in state: " + t.getState() + "\n" +
                        Arrays.stream(t.getStackTrace()).map(e -> e.toString() + "\n").reduce("", (a, b) -> a + b));
            }
        });
    }

    private boolean isStillActive(final Thread t)
    {
        return t.isAlive() && t.getState() != Thread.State.TERMINATED;
    }

    private static String toId(final Thread thread)
    {
        return thread.getId() + thread.getName();
    }

}
