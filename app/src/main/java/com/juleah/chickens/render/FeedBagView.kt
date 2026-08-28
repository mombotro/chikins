package com.juleah.chickens.render

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import com.juleah.chickens.overlay.OverlayPermission

/**
 * Three ways to interact with the bag, all requested:
 *  - Quick drag-and-release: drops feed immediately at the release point,
 *    bag springs back to its dock.
 *  - Tap: arms placement mode (onTap fires; caller shows a full-screen
 *    tap-capture via PlacementOverlay) - tap anywhere else to place there,
 *    mombotro's original click-bag-then-click-ground interaction.
 *  - Long-press then drag: relocates the bag's dock itself - no feed is
 *    dropped, and the bag stays at the new spot instead of springing back.
 *    Lets the user move the bag out of the way without it always meaning
 *    "drop feed here".
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
    private var isRelocating = false

    private val longPressHandler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable { isRelocating = true }

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
                isRelocating = false
                longPressHandler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val movedX = event.rawX - touchStartRawX
                val movedY = event.rawY - touchStartRawY
                if (!isRelocating && (kotlin.math.abs(movedX) > TOUCH_SLOP_PX || kotlin.math.abs(movedY) > TOUCH_SLOP_PX)) {
                    // Moved before the long-press fired: this is a quick drag, not a relocate.
                    longPressHandler.removeCallbacks(longPressRunnable)
                }
                layoutParams.x = layoutStartX + movedX.toInt()
                layoutParams.y = layoutStartY + movedY.toInt()
                windowManager.updateViewLayout(imageView, layoutParams)
                return true
            }
            MotionEvent.ACTION_UP -> {
                longPressHandler.removeCallbacks(longPressRunnable)
                val movedX = event.rawX - touchStartRawX
                val movedY = event.rawY - touchStartRawY
                val moved = kotlin.math.abs(movedX) > TOUCH_SLOP_PX || kotlin.math.abs(movedY) > TOUCH_SLOP_PX

                when {
                    isRelocating -> {
                        dockX = layoutParams.x
                        dockY = layoutParams.y
                    }
                    moved -> {
                        onDrop(layoutParams.x.toDouble(), layoutParams.y.toDouble())
                        layoutParams.x = dockX
                        layoutParams.y = dockY
                        windowManager.updateViewLayout(imageView, layoutParams)
                    }
                    else -> onTap()
                }
                isRelocating = false
                return true
            }
        }
        return false
    }
}
