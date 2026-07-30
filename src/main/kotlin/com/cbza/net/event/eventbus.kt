package com.cbza.net.event

object EventBus {

    // For each event type (like ChatMessageEvent), store a list of functions to call.
    private val listeners = mutableMapOf<Class<out Event>, MutableList<(Event) -> Unit>>()

    // Register interest in an event type. Example usage:
    // EventBus.subscribe(ChatMessageEvent::class.java) { event -> ... }
    fun <T : Event> subscribe(eventType: Class<T>, listener: (T) -> Unit) {
        val list = listeners.getOrPut(eventType) { mutableListOf() }

        @Suppress("UNCHECKED_CAST")
        list.add(listener as (Event) -> Unit)
    }

    // Called when something happens. Notifies every listener registered for this event's type.
    fun post(event: Event) {
        val list = listeners[event.javaClass] ?: return
        for (listener in list) {
            listener(event)
        }
    }
}