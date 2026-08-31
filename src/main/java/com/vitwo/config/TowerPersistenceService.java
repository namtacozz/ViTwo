package com.vitwo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class TowerPersistenceService {
    private static final Logger LOGGER = LoggerFactory.getLogger("CobbleTower-Persistence");
    private static final TowerPersistenceService INSTANCE = new TowerPersistenceService();
    public static TowerPersistenceService getInstance() { return INSTANCE; }

    private ExecutorService ioExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private TowerPersistenceService() {
        start();
    }

    public synchronized void start() {
        if (running.get()) return;
        running.set(true);
        ioExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "CobbleTower-AsyncIO");
            t.setDaemon(true);
            return t;
        });
        LOGGER.info("[CobbleTower] Async Persistence Service started.");
    }

    /**
     * Queues an asynchronous I/O task on the background worker thread.
     */
    public void submitAsyncTask(Runnable task) {
        if (!running.get() || ioExecutor == null || ioExecutor.isShutdown()) {
            // If service is shutting down or stopped, execute synchronously
            try {
                task.run();
            } catch (Throwable t) {
                LOGGER.error("[CobbleTower] Error executing synchronous fallback I/O task: {}", t.getMessage(), t);
            }
            return;
        }

        ioExecutor.submit(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                LOGGER.error("[CobbleTower] Error in asynchronous I/O task: {}", t.getMessage(), t);
            }
        });
    }

    /**
     * Flushes all pending write tasks and gracefully shuts down or pauses executor.
     */
    public synchronized void flushAllSync() {
        if (!running.get() || ioExecutor == null) return;
        LOGGER.info("[CobbleTower] Flushing all pending persistence tasks synchronously before server stop...");
        try {
            ioExecutor.shutdown();
            if (!ioExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.warn("[CobbleTower] Persistence worker did not terminate in 5 seconds. Forcing shutdown now.");
                ioExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOGGER.error("[CobbleTower] Persistence flush interrupted: {}", e.getMessage());
            ioExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            running.set(false);
            // Re-initialize executor for subsequent server starts (in singleplayer / reloads)
            start();
        }
    }
}
