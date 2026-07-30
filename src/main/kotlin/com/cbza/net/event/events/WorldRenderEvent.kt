package com.cbza.net.event.events

import com.cbza.net.event.Event
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f

class WorldRenderEvent (
    val bufferSource: MultiBufferSource.BufferSource,
    val matrix : Matrix4f,
    val camPos: Vec3
) : Event