package com.mombotro.chikins.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix

class SpriteSheet(context: Context, drawableResId: Int, private val frameSizePx: Int) {
    private val sheet: Bitmap = BitmapFactory.decodeResource(context.resources, drawableResId)
    private val frameCount: Int = (sheet.width / frameSizePx).coerceAtLeast(1)
    private val frames: Array<Bitmap> = Array(frameCount) { index ->
        Bitmap.createBitmap(sheet, index * frameSizePx, 0, frameSizePx, frameSizePx)
    }

    // ImageView.scaleX doesn't reliably repaint on this app's overlay windows
    // (TYPE_SYSTEM_ALERT, on old/software-rendered devices in particular) -
    // updateViewLayout() moves the window but a transform-only property change
    // isn't always recomposited, so flipped facing silently never showed on
    // real hardware. Pre-flipping actual bitmaps sidesteps that entirely: it's
    // a content change, which always triggers a normal redraw.
    private val mirroredFrames: Array<Bitmap> = Array(frameCount) { index ->
        val matrix = Matrix().apply { preScale(-1f, 1f) }
        Bitmap.createBitmap(frames[index], 0, 0, frameSizePx, frameSizePx, matrix, false)
    }

    fun frame(index: Int, mirrored: Boolean = false): Bitmap =
        (if (mirrored) mirroredFrames else frames)[index % frameCount]
}
