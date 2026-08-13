package com.cbza.net.event.events

import com.cbza.net.event.CancellableEvent
import net.minecraft.core.particles.ParticleOptions

class ParticleEvent(
    val options: ParticleOptions,
    val x: Double,
    val y: Double,
    val z: Double,
    override var isCancelled: Boolean = false
) : CancellableEvent