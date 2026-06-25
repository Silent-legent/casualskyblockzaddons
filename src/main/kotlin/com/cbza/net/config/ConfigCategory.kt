package com.cbza.net.config

import java.util.ArrayList
import java.util.function.BooleanSupplier
import java.util.function.Consumer

class ConfigCategory(val name: String) {
	val entries = ArrayList<ToggleEntry>()

	fun toggle(label: String, getter: BooleanSupplier, setter: Consumer<Boolean>): ConfigCategory {
		entries.add(ToggleEntry(label, getter, setter))
		return this
	}

	class ToggleEntry(
		val label: String,
		val getter: BooleanSupplier,
		val setter: Consumer<Boolean>
	)
}