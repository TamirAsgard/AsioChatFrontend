package com.example.asiochatfrontend.data.relay.network;

import android.util.Log;

import com.example.asiochatfrontend.app.di.ServiceModule;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Monitors TCP reachability of host:port at a fixed interval,
 * notifying exactly once on each transition lost→restored and restored→lost.
 * The underlying ScheduledExecutorService is never shut down.
 */
public class WebSocketHealthMonitor {
    public interface HealthObserver {
        void onConnectionLost();
        void onConnectionRestored();
    }

    private static final long CHECK_INTERVAL_MS = 10_000;

    private final String host;
    private final int port;
    private final HealthObserver observer;

    // single-thread executor lives until your app dies
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    // handle to the repeating task
    private ScheduledFuture<?> future;

    /** true if last check was reachable */
    private volatile boolean wasReachable = true;

    public WebSocketHealthMonitor(
            String host,
            int port,
            HealthObserver observer
    ) {
        this.host     = host;
        this.port     = port;
        this.observer = observer;
    }

    /**
     * Start (or restart) the periodic check.
     * If already running, this is a no-op.
     */
    public synchronized void start() {
        if (future != null && !future.isCancelled()) {
            return;
        }
        wasReachable = true;  // reset so next flip fires callbacks

        future = scheduler.scheduleWithFixedDelay(
                this::checkOnce,
                0,
                CHECK_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Cancel the periodic check but do not shut down the executor,
     * so you can call start() again later.
     */
    public synchronized void stop() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }

    /** one single reachability test + transition notification */
    private void checkOnce() {
        boolean ok;
        try (Socket sock = new Socket()) {
            sock.connect(new InetSocketAddress(host, port), 2_000);
            boolean currentConnectionStatus = ServiceModule.getConnectionManager().getOnlineStatus().getValue();
            if (!currentConnectionStatus) {
                try {
                    observer.onConnectionRestored();
                } catch (Exception e) {
                    Log.e("WebSocketHealthMonitor", "Error while setting online status", e);
                }
            }
            ok = true;
        } catch (IOException ignored) {
            ok = false;
        }

        if (ok) {
            if (!wasReachable) {
                wasReachable = true;
                try {
                    observer.onConnectionRestored();
                } catch (Throwable t) {
                    Log.e("WebSocketHealthMonitor", "watchdog observer error", t);
                }
            }
        } else {
            if (wasReachable) {
                wasReachable = false;
                try {
                    observer.onConnectionLost();
                } catch (Throwable t) {
                    Log.e("WebSocketHealthMonitor", "watchdog observer error", t);
                }
            }
        }
    }
}