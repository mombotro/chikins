package com.juleah.chickens.render

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.ImageView
import com.juleah.chickens.overlay.OverlayPermission

/**
 * Two ways to place feed, both requested: drag the bag directly to a spot
 * and release (onDrop fires immediately with the drop point), or tap the
 * bag once to arm placement mode (onTap fires; caller shows a full-screen
 * tap-capture via PlacementOverlay) and tap anywhere else to place there -
 * mombotro's original click-bag-then-click-ground interaction.
 */
class FeedBagView(
    context: Context,
    private val windowManager: WindowManager,
    sizePx: Int,
    private val onDrop: (x: Double, y: Double) -> Unit,
    private val onTap: () -> Unit
) {
    val imageView = ImageView(context)
    private val layoutParams = WindowManager.LayoutParams(
        sizePx,
        sizePx,
        OverlayPermission.overlayWindowType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private var dockX = 0
    private var dockY = 0
    private var touchStartRawX = 0f
    private var touchStartRawY = 0f
    private var layoutStartX = 0
    private var layoutStartY = 0
    private var added = false

    companion object {
        private const val TOUCH_SLOP_PX = 24
    }

    fun show(dockX: Int, dockY: Int) {
        this.dockX = dockX
        this.dockY = dockY
        layoutParams.x = dockX
        layoutParams.y = dockY
        windowManager.addView(imageView, layoutParams)
        added = true
        imageView.setOnTouchListener { _, event -> handleTouch(event) }
    }

    fun remove() {
        if (added) {
            windowManager.removeView(imageView)
            added = false
        }
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartRawX = event.rawX
                touchStartRawY = event.rawY
                layoutStartX = layoutParams.x
                layoutStartY = layoutParams.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                layoutParams.x = layoutStartX + (event.rawX - touchStartRawX).toInt()
                layoutParams.y = layoutStartY + (event.rawY - touchStartRawY).toInt()
                windowManager.updateViewLayout(imageView, layoutParams)
                return true
            }
            MotionEvent.ACTION_UP -> {
                val movedX = event.rawX - touchStartRawX
                val movedY = event.rawY - touchStartRawY
                if (kotlin.math.abs(movedX) > TOUCH_SLOP_PX || kotlin.math.abs(movedY) > TOUCH_SLOP_PX) {
                    onDrop(layoutParams.x.toDouble(), layoutParams.y.toDouble())
                    layoutParams.x = dockX
                    layoutParams.y = dockY
                    windowManager.updateViewLayout(imageView, layoutParams)
                } else {
                    onTap()
                }
                return true
            }
        }
        return false
    }
}
