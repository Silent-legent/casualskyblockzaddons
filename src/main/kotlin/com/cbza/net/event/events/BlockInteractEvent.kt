package com.cbza.net.event.events

import com.cbza.net.event.CancellableEvent
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand

class BlockInteractEvent(
    val blockPos: BlockPos,
    val direction: Direction,
    val hand: InteractionHand,
    override var isCancelled: Boolean = false
) : CancellableEvent