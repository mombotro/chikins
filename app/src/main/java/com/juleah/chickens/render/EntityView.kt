package com.juleah.chickens.render

import android.content.Context
import android.graphics.PixelFormat
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import com.juleah.chickens.overlay.OverlayPermission

class EntityView(
    context: Context,
    private val windowManager: WindowManager,
    sizePx: Int
) {
    val imageView = ImageView(context)
    private val layoutParams = WindowManager.LayoutParams(
        sizePx,
        sizePx,
        OverlayPermission.overlayWindowType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }
    private var added = false
    private var currentX = 0.0
    private var currentY = 0.0
    private var lastShowAtMs = 0L

    /**
     * Moves the window straight to the new position (one updateViewLayout
     * call, same cost as before) but visually offsets the view with
     * translationX/Y so it appears to start from the old spot, then
     * animates that offset back to zero. translationX/Y is a GPU-composited
     * view transform, not a WindowManager call, so this doesn't add IPC
     * overhead - the tick loop itself is already the bottleneck (real tick
     * cost runs 150-450ms, well above the 33ms nominal rate), so without
     * this every move looks like a teleport rather than a glide.
     */
    fun show(x: Double, y: Double) {
        if (!added) {
            currentX = x
            currentY = y
            layoutParams.x = x.toInt()
            layoutParams.y = y.toInt()
            windowManager.addView(imageView, layoutParams)
            added = true
            lastShowAtMs = SystemClock.elapsedRealtime()
            return
        }

        val now = SystemClock.elapsedRealtime()
        val intervalMs = (now - lastShowAtMs).coerceIn(16L, 500L)
        lastShowAtMs = now

        val dx = (x - currentX).toFloat()
        val dy = (y - currentY).toFloat()
        currentX = x
        currentY = y

        imageView.animate().cancel()
        layoutParams.x = x.toInt()
        layoutParams.y = y.toInt()
        windowManager.updateViewLayout(imageView, layoutParams)

        imageView.translationX = -dx
        imageView.translationY = -dy
        imageView.animate()
            .translationX(0f)
            .translationY(0f)
            .setDuration(intervalMs)
            .setInterpolator(LinearInterpolator())
            .start()
    }

    fun remove() {
        if (added) {
            imageView.animate().cancel()
            windowManager.removeView(imageView)
            added = false
        }
    }

    /** Re-adds this window so it stacks above every other window already added - windows added later render on top. */
    fun bringToFront() {
        if (added) {
            windowManager.removeView(imageView)
            windowManager.addView(imageView, layoutParams)
        }
    }
}
