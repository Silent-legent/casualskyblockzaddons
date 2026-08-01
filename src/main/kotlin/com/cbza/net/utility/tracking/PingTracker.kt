package com.cbza.net.utility.tracking

object PingTracker {
    @Volatile private var latestPing: Long? = null

    fun onPongReceived(deltaMs: Long) {
        latestPing = deltaMs
    }

    /** Real measured ping, or null if we haven't seen a pong yet */
    fun getPing(): Long? = latestPing
}