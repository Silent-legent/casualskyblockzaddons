package com.cbza.net.event.events

import com.cbza.net.event.CancellableEvent
import net.minecraft.network.chat.Component

class ChatEvent @JvmOverloads constructor(
    val component: Component,
    val text: String = component.string,
    override var isCancelled: Boolean = false
) : CancellableEvent