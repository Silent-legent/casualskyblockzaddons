package com.cbza.net.event.events

import com.cbza.net.event.Event
import net.minecraft.core.BlockPos

class BlockInteractEvent (
    val blockPos: BlockPos,
) : Event