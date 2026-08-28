package com.juleah.chickens.render

import android.content.Context
import android.view.WindowManager
import com.juleah.chickens.R
import com.juleah.chickens.sim.ChickenConfig
import com.juleah.chickens.sim.Flock

class OverlayRenderer(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private val chickenSprite = SpriteSheet(context, R.drawable.chicken, ChickenConfig.SIZE_PX)
    private val chickenViews = mutableMapOf<Long, EntityView>()

    fun render(flock: Flock) {
        val liveIds = flock.chickens.map { it.id }.toSet()
        chickenViews.keys.filterNot { it in liveIds }.forEach { staleId ->
            chickenViews.remove(staleId)?.remove()
        }

        flock.chickens.forEach { chicken ->
            val view = chickenViews.getOrPut(chicken.id) {
                EntityView(context, windowManager, ChickenConfig.SIZE_PX)
            }
            view.imageView.setImageBitmap(chickenSprite.frame(0))
            view.show(chicken.x, chicken.y)
        }
    }

    fun clear() {
        chickenViews.values.forEach { it.remove() }
        chickenViews.clear()
    }
}
