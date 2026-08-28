package com.juleah.chickens.render

import android.content.Context
import android.view.WindowManager
import com.juleah.chickens.R
import com.juleah.chickens.sim.ChickConfig
import com.juleah.chickens.sim.ChickenAnimState
import com.juleah.chickens.sim.ChickenConfig
import com.juleah.chickens.sim.EggConfig
import com.juleah.chickens.sim.FeedConfig
import com.juleah.chickens.sim.Flock

class OverlayRenderer(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private val chickenSprite = SpriteSheet(context, R.drawable.chicken, ChickenConfig.SIZE_PX)
    private val chickenViews = mutableMapOf<Long, EntityView>()
    private val sittingChickenIds = mutableSetOf<Long>()
    private val peckingChickenIds = mutableSetOf<Long>()

    private val chickSprite = SpriteSheet(context, R.drawable.chick, ChickConfig.SIZE_PX)
    private val chickViews = mutableMapOf<Long, EntityView>()

    private val eggSprite = SpriteSheet(context, R.drawable.egg, EggConfig.SIZE_PX)
    private val eggViews = mutableMapOf<Long, EntityView>()

    private val feedSprite = SpriteSheet(context, R.drawable.feed, FeedConfig.SIZE_PX)
    private val feedPileViews = mutableMapOf<Long, EntityView>()

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
            val isSittingNow = chicken.animState == ChickenAnimState.SITTING
            if (isSittingNow && sittingChickenIds.add(chicken.id)) {
                // Just started sitting: force this window above the egg's, which was
                // added later (eggs don't exist until laid) and would otherwise stack
                // on top per Android's last-added-wins ordering for sibling overlay
                // windows.
                view.bringToFront()
            } else if (!isSittingNow) {
                sittingChickenIds.remove(chicken.id)
            }

            val isPeckingNow = chicken.animState == ChickenAnimState.PECKING
            if (isPeckingNow && peckingChickenIds.add(chicken.id)) {
                // Same reasoning as sitting, above: feed piles are also added
                // after chickens exist, so without this the feed would render
                // in front of a chicken pecking at it.
                view.bringToFront()
            } else if (!isPeckingNow) {
                peckingChickenIds.remove(chicken.id)
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

        val livePileIds = flock.feedPiles.map { it.id }.toSet()
        feedPileViews.keys.filterNot { it in livePileIds }.forEach { staleId ->
            feedPileViews.remove(staleId)?.remove()
        }

        flock.feedPiles.forEach { pile ->
            val view = feedPileViews.getOrPut(pile.id) {
                EntityView(context, windowManager, FeedConfig.SIZE_PX)
            }
            view.imageView.setImageBitmap(feedSprite.frame(feedFrameIndex(pile.amount)))
            view.show(pile.x, pile.y)
        }
    }

    private fun feedFrameIndex(amount: Int): Int {
        val fraction = amount.toDouble() / FeedConfig.INITIAL_AMOUNT
        return when {
            fraction > FeedConfig.FULL_FRAME_THRESHOLD -> 0
            fraction > FeedConfig.HALF_FRAME_THRESHOLD -> 1
            else -> 2
        }
    }

    fun clear() {
        chickenViews.values.forEach { it.remove() }
        chickenViews.clear()
        chickViews.values.forEach { it.remove() }
        chickViews.clear()
        eggViews.values.forEach { it.remove() }
        eggViews.clear()
        feedPileViews.values.forEach { it.remove() }
        feedPileViews.clear()
    }
}
