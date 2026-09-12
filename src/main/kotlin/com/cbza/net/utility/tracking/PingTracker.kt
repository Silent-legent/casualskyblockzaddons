package com.cbza.net.utility.tracking

import com.cbza.net.event.EventBus
import com.cbza.net.event.events.ServerJoinEvent

object PingTracker {

    @Volatile private var latestPing: Long? = null

    init {
        // Automatically reset stale ping when switching servers or lobbies
        EventBus.subscribe<ServerJoinEvent> { reset() }
    }

    fun onPongReceived(deltaMs: Long) {
        if (deltaMs < 0) return // Ignores invalid timing deltas
        latestPing = deltaMs
    }

    fun reset() {
        latestPing = null
    }

    fun getPing(): Long? = latestPing

    fun getPingOrDefault(default: Long = 0L): Long = latestPing ?: default
}