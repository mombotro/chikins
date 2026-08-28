package com.juleah.chickens.sim

enum class ChickenAnimState { IDLE, WALKING, PECKING, JUMPING, SITTING }

class ChickenEntity(
    val id: Long,
    var x: Double,
    var y: Double,
    birthTimeMs: Long,
    private val rng: Rng
) {
    val deathAtMs: Long = birthTimeMs + rng.nextLongInRange(ChickenConfig.MIN_LIFESPAN_MS, ChickenConfig.MAX_LIFESPAN_MS)
    var isDead: Boolean = false
        private set

    var animState: ChickenAnimState = ChickenAnimState.IDLE
        private set
    var facingRight: Boolean = true
        private set

    private var velocityX: Double = (rng.nextDouble() - 0.5) * ChickenConfig.MAX_VELOCITY
    private var velocityY: Double = (rng.nextDouble() - 0.5) * ChickenConfig.MAX_VELOCITY

    private var isIdling = false
    private var idleRemainingMs = 0L
    private var isPecking = false
    private var peckRemainingMs = 0L
    private var isJumping = false
    private var jumpRemainingMs = 0L
    private var feedPeckRemainingMs = 0L
    private var targetFeedId: Long? = null

    private var lastAnimState = ChickenAnimState.IDLE
    private var animFrameIndex = 0
    private var animFrameTimeMs = 0L

    fun tick(nowMs: Long) {
        if (nowMs >= deathAtMs) {
            isDead = true
        }
    }

    fun currentTargetFeedId(): Long? = targetFeedId
    fun clearTargetFeed() { targetFeedId = null }
    fun rushToFeed(feedId: Long) {
        targetFeedId = feedId
        isIdling = false
        isPecking = false
    }

    /**
     * Actual sprite-sheet frame index for the current animation state/timing.
     * Indexed modulo the current state's frame count: wander() can change
     * animState after advanceAnimation() already ran this call (e.g. the
     * peck/idle-chance branches), leaving animFrameIndex sized for the
     * *previous* state's (possibly larger) frame array until the next
     * advanceAnimation() call catches up and resets it.
     */
    fun currentSpriteFrame(): Int {
        val frames = framesFor(animState)
        return frames[animFrameIndex % frames.size]
    }

    /** Called instead of wander() while this chicken is sitting on an egg. */
    fun holdSittingPose(eggX: Double, eggY: Double, deltaMs: Long) {
        x = eggX
        y = eggY
        animState = ChickenAnimState.SITTING
        advanceAnimation(deltaMs)
    }

    /** Ported from mombotro's chicken.js wander(), with time-based (not tick-based) movement. */
    fun wander(deltaMs: Long, screenWidthPx: Double, screenHeightPx: Double) {
        advanceAnimation(deltaMs)

        if (isJumping) {
            jumpRemainingMs -= deltaMs
            if (jumpRemainingMs <= 0) {
                isJumping = false
                animState = movingAnimState()
            }
            return
        }

        if (isPecking) {
            peckRemainingMs -= deltaMs
            if (peckRemainingMs <= 0) {
                isPecking = false
                animState = movingAnimState()
            }
            return
        }

        if (isIdling) {
            idleRemainingMs -= deltaMs
            if (idleRemainingMs <= 0) {
                isIdling = false
                velocityX = (rng.nextDouble() - 0.5) * ChickenConfig.MAX_VELOCITY
                velocityY = (rng.nextDouble() - 0.5) * ChickenConfig.MAX_VELOCITY
            } else {
                return
            }
        }

        if (rng.nextDouble() < ChickenConfig.DIRECTION_CHANGE_CHANCE) {
            velocityX = (rng.nextDouble() - 0.5) * ChickenConfig.MAX_VELOCITY
            velocityY = (rng.nextDouble() - 0.5) * ChickenConfig.MAX_VELOCITY
        }

        if (rng.nextDouble() < ChickenConfig.PECK_CHANCE) {
            isPecking = true
            peckRemainingMs = ChickenConfig.PECK_DURATION_MS
            animState = ChickenAnimState.PECKING
            return
        }

        if (rng.nextDouble() < ChickenConfig.IDLE_CHANCE) {
            isIdling = true
            idleRemainingMs = ChickenConfig.IDLE_MIN_DURATION_MS +
                (rng.nextDouble() * (ChickenConfig.IDLE_MAX_DURATION_MS - ChickenConfig.IDLE_MIN_DURATION_MS)).toLong()
            animState = ChickenAnimState.IDLE
            return
        }

        val seconds = deltaMs / 1000.0
        x += velocityX * seconds
        y += velocityY * seconds
        facingRight = velocityX > 0.1

        val size = ChickenConfig.SIZE_PX
        if (x <= 0 || x >= screenWidthPx - size) {
            velocityX = -velocityX
            x = x.coerceIn(0.0, screenWidthPx - size)
        }
        if (y <= 0 || y >= screenHeightPx - size) {
            velocityY = -velocityY
            y = y.coerceIn(0.0, screenHeightPx - size)
        }

        animState = movingAnimState()
    }

    fun jump() {
        if (isJumping || isPecking) return
        isJumping = true
        jumpRemainingMs = ChickenConfig.JUMP_DURATION_MS
        animState = ChickenAnimState.JUMPING
    }

    fun moveTowardFeed(deltaMs: Long, feedX: Double, feedY: Double): Boolean {
        advanceAnimation(deltaMs)

        if (feedPeckRemainingMs > 0) {
            feedPeckRemainingMs -= deltaMs
            animState = ChickenAnimState.PECKING
            return false
        }

        val dx = feedX - x
        val dy = feedY - y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

        if (distance <= 20.0) {
            feedPeckRemainingMs = ChickenConfig.PECK_DURATION_MS
            animState = ChickenAnimState.PECKING
            return true
        }

        val seconds = deltaMs / 1000.0
        x += (dx / distance) * ChickenConfig.FEED_APPROACH_SPEED * seconds
        y += (dy / distance) * ChickenConfig.FEED_APPROACH_SPEED * seconds
        facingRight = dx > 0
        animState = ChickenAnimState.WALKING
        return false
    }

    private fun movingAnimState(): ChickenAnimState =
        if (kotlin.math.abs(velocityX) + kotlin.math.abs(velocityY) > ChickenConfig.MOVING_SPEED_THRESHOLD) {
            ChickenAnimState.WALKING
        } else {
            ChickenAnimState.IDLE
        }

    private fun advanceAnimation(deltaMs: Long) {
        if (animState != lastAnimState) {
            lastAnimState = animState
            animFrameIndex = 0
            animFrameTimeMs = 0L
        }
        animFrameTimeMs += deltaMs
        val speed = speedFor(animState)
        if (animFrameTimeMs >= speed) {
            animFrameTimeMs = 0L
            val frames = framesFor(animState)
            animFrameIndex = (animFrameIndex + 1) % frames.size
        }
    }

    private fun framesFor(state: ChickenAnimState): IntArray = when (state) {
        ChickenAnimState.IDLE -> ChickenConfig.IDLE_FRAMES
        ChickenAnimState.WALKING -> ChickenConfig.WALKING_FRAMES
        ChickenAnimState.PECKING -> ChickenConfig.PECKING_FRAMES
        ChickenAnimState.JUMPING -> ChickenConfig.JUMPING_FRAMES
        ChickenAnimState.SITTING -> ChickenConfig.SITTING_FRAMES
    }

    private fun speedFor(state: ChickenAnimState): Long = when (state) {
        ChickenAnimState.IDLE -> ChickenConfig.IDLE_FRAME_SPEED_MS
        ChickenAnimState.WALKING -> ChickenConfig.WALKING_FRAME_SPEED_MS
        ChickenAnimState.PECKING -> ChickenConfig.PECKING_FRAME_SPEED_MS
        ChickenAnimState.JUMPING -> ChickenConfig.JUMPING_FRAME_SPEED_MS
        ChickenAnimState.SITTING -> ChickenConfig.SITTING_FRAME_SPEED_MS
    }
}
