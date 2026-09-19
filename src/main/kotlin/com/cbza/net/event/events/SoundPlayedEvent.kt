package com.cbza.net.event.events

import com.cbza.net.event.CancellableEvent
import net.minecraft.sounds.SoundEvent
import net.minecraft.world.phys.Vec3

class SoundPlayedEvent (
    val sound: SoundEvent,
    val pos: Vec3,
    val volume: Float,
    val pitch: Float,
    override var isCancelled: Boolean = false
    ) : CancellableEvent