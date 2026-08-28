package com.juleah.chickens.render

import android.content.Context
import android.graphics.PixelFormat
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.juleah.chickens.overlay.OverlayPermission

/**
 * A full-screen, touchable-but-invisible window used only briefly, between
 * the feed bag being tapped and the next tap landing - the only way to
 * capture "tap anywhere on screen" given every other window in this app is
 * a small per-entity window that deliberately doesn't cover the screen (so
 * the rest of the screen stays usable). Disarms itself the instant a tap
 * lands, so it never blocks anything longer than that one gesture.
 */
class PlacementOverlay(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onTapPlace: (x: Double, y: Double) -> Unit
) {
    private var view: View? = null

    val isArmed: Boolean get() = view != null

    fun arm() {
        if (view != null) return
        val captureView = View(context)
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            OverlayPermission.overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        captureView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                onTapPlace(event.rawX.toDouble(), event.rawY.toDouble())
                disarm()
                true
            } else {
                false
            }
        }
        windowManager.addView(captureView, layoutParams)
        view = captureView
    }

    fun disarm() {
        view?.let { windowManager.removeView(it) }
        view = null
    }
}
