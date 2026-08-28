package com.juleah.chickens.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

class SpriteSheet(context: Context, drawableResId: Int, private val frameSizePx: Int) {
    private val sheet: Bitmap = BitmapFactory.decodeResource(context.resources, drawableResId)
    private val frameCount: Int = (sheet.width / frameSizePx).coerceAtLeast(1)
    private val frames: Array<Bitmap> = Array(frameCount) { index ->
        Bitmap.createBitmap(sheet, index * frameSizePx, 0, frameSizePx, frameSizePx)
    }

    fun frame(index: Int): Bitmap = frames[index % frames.size]
}
