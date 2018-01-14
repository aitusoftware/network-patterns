package com.aitusoftware.network.patterns.app;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class Scheduler
{
    private Scheduler() {}

    static void delayedCancel(
            final Future<?> future, final long duration, final TimeUnit unit,
            final ExecutorService executor)
    {
        executor.submit(() -> {
            final long start = System.currentTimeMillis();
            try
            {
                Thread.sleep(unit.toMillis(duration));
            }
            catch (InterruptedException e)
            {
                System.err.printf("Interrupted whilst sleeping after %dms: %s%n",
                        System.currentTimeMillis() - start, e.getMessage());
            }
            System.out.printf("Cancelling Future after sleeping for %dms%n",
                    System.currentTimeMillis() - start);
            future.cancel(true);
        });
    }
}
