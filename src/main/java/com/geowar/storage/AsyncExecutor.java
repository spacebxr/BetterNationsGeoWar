package com.geowar.storage;

import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Runs database work on a dedicated single-thread executor so JDBC never touches
 * the server main thread. Results come back as {@link CompletableFuture}s that
 * callers can complete on the main thread via the scheduler when they need to
 * touch Bukkit state.
 */
public final class AsyncExecutor {

    private final Plugin plugin;
    private final ExecutorService executor;

    public AsyncExecutor(Plugin plugin) {
        this.plugin = plugin;
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "GeoWar-DB");
            thread.setDaemon(true);
            return thread;
        };
        this.executor = Executors.newSingleThreadExecutor(factory);
    }

    public <T> CompletableFuture<T> supply(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable throwable) {
                plugin.getLogger().log(Level.SEVERE, "Async database task failed", throwable);
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    public CompletableFuture<Void> run(Runnable runnable) {
        return supply(() -> {
            runnable.run();
            return null;
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
