package com.cbza.net.user_interface

class UiButton(var x: Int, var y: Int, val width: Int, val height: Int, val label: String, val onClick: () -> Unit) {
    // TODO 1: isHovered(mouseX, mouseY): Boolean
    // hint: is the mouse inside the rectangle? (4 comparisons, think about < vs <=)

    // TODO 2: render(ctx: RenderContext, mouseX: Int, mouseY: Int)
    // hint: draw a filled rect, pick a different color when hovered, then draw the label

    // TODO 3: click(mouseX: Int, mouseY: Int): Boolean
    // hint: if hovered, run onClick. Return true if you used the click, false if not
}