package com.cbza.net.event.events

import com.cbza.net.event.Event
//? if >=26.2 {
/*import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.SubmitNodeCollector
*///?} else {
import net.minecraft.client.renderer.MultiBufferSource
import org.joml.Matrix4f
//?}
import net.minecraft.world.phys.Vec3

class RenderEvent (
    //? if >=26.2 {
    /*    val collector: SubmitNodeCollector,
    val poseStack: PoseStack,
    *///?} else {
    val bufferSource: MultiBufferSource.BufferSource,
    val matrix : Matrix4f,
    //?}
    val camPos: Vec3
) : Event