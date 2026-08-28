package com.juleah.chickens.render

import android.content.Context
import android.view.WindowManager
import com.juleah.chickens.R
import com.juleah.chickens.sim.ChickConfig
import com.juleah.chickens.sim.ChickenConfig
import com.juleah.chickens.sim.EggConfig
import com.juleah.chickens.sim.Flock

class OverlayRenderer(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private val chickenSprite = SpriteSheet(context, R.drawable.chicken, ChickenConfig.SIZE_PX)
    private val chickenViews = mutableMapOf<Long, EntityView>()

    private val chickSprite = SpriteSheet(context, R.drawable.chick, ChickConfig.SIZE_PX)
    private val chickViews = mutableMapOf<Long, EntityView>()

    private val eggSprite = SpriteSheet(context, R.drawable.egg, EggConfig.SIZE_PX)
    private val eggViews = mutableMapOf<Long, EntityView>()

    fun render(flock: Flock, nowMs: Long) {
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
            view.imageView.scaleX = if (chicken.facingRight) -1f else 1f
            view.show(chicken.x, chicken.y)
        }

        val liveChickIds = flock.chicks.map { it.id }.toSet()
        chickViews.keys.filterNot { it in liveChickIds }.forEach { staleId ->
            chickViews.remove(staleId)?.remove()
        }

        flock.chicks.forEach { chick ->
            val view = chickViews.getOrPut(chick.id) {
                EntityView(context, windowManager, ChickConfig.SIZE_PX).also {
                    it.imageView.setOnClickListener { _ -> chick.handleTap() }
                }
            }
            view.imageView.setImageBitmap(chickSprite.frame(chick.currentSpriteFrame()))
            view.imageView.scaleX = if (chick.facingRight) -1f else 1f
            view.show(chick.x, chick.y)
        }

        val liveEggIds = flock.eggs.map { it.id }.toSet()
        eggViews.keys.filterNot { it in liveEggIds }.forEach { staleId ->
            eggViews.remove(staleId)?.remove()
        }

        flock.eggs.forEach { egg ->
            val view = eggViews.getOrPut(egg.id) {
                EntityView(context, windowManager, EggConfig.SIZE_PX)
            }
            view.imageView.setImageBitmap(eggSprite.frame(egg.currentSpriteFrame(nowMs)))
            view.show(egg.x, egg.y)
        }
    }

    fun clear() {
        chickenViews.values.forEach { it.remove() }
        chickenViews.clear()
        chickViews.values.forEach { it.remove() }
        chickViews.clear()
        eggViews.values.forEach { it.remove() }
        eggViews.clear()
    }
}
