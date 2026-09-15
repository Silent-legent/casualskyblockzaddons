package com.cbza.net.event.events

import com.cbza.net.event.Event
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.world.phys.Vec3

class RenderEvent(
    val collector: SubmitNodeCollector,
    val poseStack: PoseStack,
    val camPos: Vec3,
) : Event