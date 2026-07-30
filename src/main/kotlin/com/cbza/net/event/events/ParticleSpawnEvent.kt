package com.cbza.net.event.events

import com.cbza.net.event.Event
import net.minecraft.core.particles.ParticleOptions

class ParticleSpawnEvent(
    val options: ParticleOptions,
    val x: Double,
    val y: Double,
    val z: Double
) : Event