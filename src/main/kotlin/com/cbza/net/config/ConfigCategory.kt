package com.cbza.net.config

import kotlin.reflect.KMutableProperty0

class ConfigCategory(val name: String) {
	val entries = mutableListOf<ToggleEntry>()

	fun toggle(
		label: String,
		property: KMutableProperty0<Boolean>,
		description: String = ""
	): ConfigCategory {
		entries.add(ToggleEntry(label, { property.get() }, { property.set(it) }, description))
		return this
	}

	fun toggle(
		label: String,
		getter: () -> Boolean,
		setter: (Boolean) -> Unit,
		description: String = ""
	): ConfigCategory {
		entries.add(ToggleEntry(label, getter, setter, description))
		return this
	}

	class ToggleEntry(
		val label: String,
		val getter: () -> Boolean,
		val setter: (Boolean) -> Unit,
		val description: String = ""
	)
}