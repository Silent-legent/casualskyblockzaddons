package com.cbza.net.event

interface Event

interface CancellableEvent : Event {
    var isCancelled: Boolean

    fun cancel() {
        isCancelled = true
    }
}