package com.cbza.net.utility.tracking

import com.cbza.net.event.EventBus
import com.cbza.net.event.events.ServerJoinEvent

/**
 * Tracks client-to-server network latency (ping) measured via packet RTT.
 */
object PingTracker {

    @Volatile private var latestPing: Long? = null

    init {
        // Automatically reset stale ping when switching servers or lobbies
        EventBus.subscribe<ServerJoinEvent> { reset() }
    }

    /**
     * Updates the latest ping latency in milliseconds.
     */
    fun onPongReceived(deltaMs: Long) {
        if (deltaMs < 0) return // Ignores invalid timing deltas
        latestPing = deltaMs
    }

    /**
     * Resets recorded ping data. Called automatically on server change.
     */
    fun reset() {
        latestPing = null
    }

    /** Real measured ping, or null if no pong has been received yet */
    fun getPing(): Long? = latestPing

    /** Returns the measured ping, or [default] if no sample exists */
    fun getPingOrDefault(default: Long = 0L): Long = latestPing ?: default
}