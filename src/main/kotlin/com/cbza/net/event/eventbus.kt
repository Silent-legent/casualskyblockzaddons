package com.cbza.net.event

object EventBus {

    private val listeners = mutableMapOf<Class<out Event>, MutableList<(Event) -> Unit>>()

    // Generic Kotlin helper - no more ::class.java needed!
    inline fun <reified T : Event> subscribe(noinline listener: (T) -> Unit) {
        subscribe(T::class.java, listener)
    }

    fun <T : Event> subscribe(eventType: Class<T>, listener: (T) -> Unit) {
        val list = listeners.getOrPut(eventType) { mutableListOf() }

        @Suppress("UNCHECKED_CAST")
        list.add(listener as (Event) -> Unit)
    }

    fun post(event: Event) {
        val list = listeners[event.javaClass] ?: return

        // Copy list to safely loop even if subscriptions change during execution
        for (listener in ArrayList(list)) {
            listener(event)
        }
    }
}