package com.juleah.chickens.render

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
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
}
