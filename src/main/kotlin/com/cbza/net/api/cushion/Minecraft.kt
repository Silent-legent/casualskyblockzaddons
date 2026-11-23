package com.cbza.net.api.cushion

val mc get() = Minecraft.getInstance()
val screen: Screen? get() = /*? if >=26.2 {*/ /*mc.gui.screen() *//*?} else {*/ mc.screen /*?}*/

fun setScreen(newScreen: Screen?) {
    /*? if >=26.2 {*/ /*mc.gui.setScreen(newScreen) *//*?} else {*/ mc.setScreen(newScreen) /*?}*/
}

typealias Minecraft = net.minecraft.client.Minecraft
typealias Identifier = net.minecraft.resources.Identifier
typealias MCSoundEvents = net.minecraft.sounds.SoundEvents