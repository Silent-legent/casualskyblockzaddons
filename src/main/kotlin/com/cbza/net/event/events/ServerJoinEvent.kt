package com.cbza.net.event.events

import com.cbza.net.event.Event

object ServerJoinEvent : Event

// 		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
//			MiningAbilityTracker.onServerJoin()
//		}