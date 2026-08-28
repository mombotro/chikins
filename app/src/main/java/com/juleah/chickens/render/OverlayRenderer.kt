package com.juleah.chickens.render

import android.content.Context
import android.view.WindowManager
import com.juleah.chickens.R
import com.juleah.chickens.sim.ChickConfig
import com.juleah.chickens.sim.ChickenConfig
import com.juleah.chickens.sim.Flock

class OverlayRenderer(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private val chickenSprite = SpriteSheet(context, R.drawable.chicken, ChickenConfig.SIZE_PX)
    private val chickenViews = mutableMapOf<Long, EntityView>()

    private val chickSprite = SpriteSheet(context, R.drawable.chick, ChickConfig.SIZE_PX)
    private val chickViews = mutableMapOf<Long, EntityView>()

    fun render(flock: Flock) {
        val liveIds = flock.chickens.map { it.id }.toSet()
        chickenViews.keys.filterNot { it in liveIds }.forEach { staleId ->
            chickenViews.remove(staleId)?.remove()
        }

        flock.chickens.forEach { chicken ->
            val view = chickenViews.getOrPut(chicken.id) {
                EntityView(context, windowManager, ChickenConfig.SIZE_PX).also {
                    it.imageView.setOnClickListener { _ -> chicken.jump() }
                }
            }
            view.imageView.setImageBitmap(chickenSprite.frame(chicken.currentSpriteFrame()))
            view.show(chicken.x, chicken.y)
        }

        val liveChickIds = flock.chicks.map { it.id }.toSet()
        chickViews.keys.filterNot { it in liveChickIds }.forEach { staleId ->
            chickViews.remove(staleId)?.remove()
        }

        flock.chicks.forEach { chick ->
            val view = chickViews.getOrPut(chick.id) {
                EntityView(context, windowManager, ChickConfig.SIZE_PX).also {
                    it.imageView.setOnClickListener { _ -> chick.runAway() }
                }
            }
            view.imageView.setImageBitmap(chickSprite.frame(chick.currentSpriteFrame()))
            view.show(chick.x, chick.y)
        }
    }

    fun clear() {
        chickenViews.values.forEach { it.remove() }
        chickenViews.clear()
        chickViews.values.forEach { it.remove() }
        chickViews.clear()
    }
}
