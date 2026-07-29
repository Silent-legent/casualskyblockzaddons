package com.cbza.net.utility

// Estimates the server's current TPS (ticks per second. basically how well the
// server is keeping up / how laggy it is), by timing how far apart game updates
// arrive and smoothing that out over recent samples.
object TpsTracker {

    // Ring buffer of recent tick deltas (ms), used to smooth the TPS reading
    private const val SAMPLE_COUNT = 20
    private const val MIN_DELTA_MS = 100L

    private val samples = DoubleArray(SAMPLE_COUNT)
    private var sampleIndex = 0
    private var samplesFilled = 0

    private var prevTime = 0L

    @Volatile private var latestTps: Double? = null

    // Called on every game update. Measures the time since the last update and
    // uses it to refresh the smoothed TPS estimate.
    @Synchronized
    fun onTimeUpdate() {
        val now = System.currentTimeMillis()

        if (prevTime != 0L) {
            val delta = now - prevTime

            // Skip suspiciously fast updates (duplicate/burst events)
            if (delta < MIN_DELTA_MS) {
                return
            }

            samples[sampleIndex] = delta.toDouble()
            sampleIndex = (sampleIndex + 1) % SAMPLE_COUNT
            if (samplesFilled < SAMPLE_COUNT) samplesFilled++

            val avgDelta = samples.take(samplesFilled).average()
            latestTps = (20000.0 / avgDelta).coerceIn(0.0, 20.0)
        }

        prevTime = now
    }

    /** Call this on (re)join/world-change so stale data doesn't pollute the next reading */
    @Synchronized
    fun reset() {
        samples.fill(0.0)
        sampleIndex = 0
        samplesFilled = 0
        prevTime = 0L
        latestTps = null
    }

    /** Returns null if no valid samples have been collected yet */
    fun getAverageTps(): Double? = latestTps
}