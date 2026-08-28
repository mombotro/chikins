package com.mombotro.chikins.render

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageView
import com.mombotro.chikins.overlay.OverlayPermission

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

    // A translationX/Y-based glide was tried here to smooth over the
    // irregular tick interval, but sub-pixel GPU compositing blurred these
    // small pixel-art sprites and produced visible ghosting/ofsetting.
    // Reverted to a direct position set - jerkier, but crisp.
    fun show(x: Double, y: Double) {
        layoutParams.x = x.toInt()
        layoutParams.y = y.toInt()
        if (!added) {
            windowManager.addView(imageView, layoutParams)
            added = true
        } else {
            windowManager.updateViewLayout(imageView, layoutParams)
        }
    }

    fun remove() {
        if (added) {
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
