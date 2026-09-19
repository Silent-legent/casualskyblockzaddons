package com.cbza.net.event.events.compat

import com.cbza.net.event.EventBus
import com.cbza.net.event.events.SoundPlayedEvent

import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.minecraft.sounds.SoundPlayedEvent as SoundApi

object SkyblockApiBridge {
    init {
        SkyBlockAPI.eventBus.register(this)
    }
    @Subscription
    fun onSoundReceive(event: SoundApi) {
        EventBus.post(SoundPlayedEvent(event.sound, event.pos, event.volume, event.pitch))
    }
}