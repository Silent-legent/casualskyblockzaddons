package com.cbza.net.utility.tracking

import com.cbza.net.event.EventBus
import com.cbza.net.event.events.ServerJoinEvent

/**
 * Tracks and calculates real-time server TPS (Ticks Per Second) using a ring buffer
 * of packet time deltas.
 */
object TpsTracker {

    private const val SAMPLE_COUNT = 20
    private const val MIN_DELTA_MS = 100L // Ignores duplicate/burst packets

    private val samples = DoubleArray(SAMPLE_COUNT)
    private var sampleIndex = 0
    private var samplesFilled = 0

    private var prevTime = 0L

    @Volatile private var latestTps: Double? = null

    init {
        // Automatically reset stale sample data whenever changing servers or lobbies
        EventBus.subscribe<ServerJoinEvent> { reset() }
    }

    /**
     * Called whenever a packet updating world time arrives from the server.
     * Assumes a standard packet interval of 20 server ticks.
     */
    @Synchronized
    fun onTimeUpdate() {
        val now = System.currentTimeMillis()

        if (prevTime != 0L) {
            val delta = now - prevTime

            // Skip burst/duplicate packets without updating prevTime so the next interval measures correctly
            if (delta < MIN_DELTA_MS) {
                return
            }

            samples[sampleIndex] = delta.toDouble()
            sampleIndex = (sampleIndex + 1) % SAMPLE_COUNT
            if (samplesFilled < SAMPLE_COUNT) samplesFilled++

            // Zero-allocation sum
            var sum = 0.0
            for (i in 0 until samplesFilled) {
                sum += samples[i]
            }

            val avgDelta = sum / samplesFilled

            // 20 ticks * 1000 ms / avgDelta ms = 20000.0 / avgDelta
            latestTps = (20000.0 / avgDelta).coerceIn(0.0, 20.0)
        }

        prevTime = now
    }

    /**
     * Clears all recorded samples. Call on world transition/warp.
     */
    @Synchronized
    fun reset() {
        samples.fill(0.0)
        sampleIndex = 0
        samplesFilled = 0
        prevTime = 0L
        latestTps = null
    }

    /**
     * Returns the smoothed server TPS, or null if insufficient packets have arrived.
     */
    fun getAverageTps(): Double? = latestTps
}