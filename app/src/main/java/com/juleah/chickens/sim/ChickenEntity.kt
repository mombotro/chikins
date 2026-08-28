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
    private var jumpProgress = 0.0
    private var jumpStartX = 0.0
    private var jumpStartY = 0.0
    private var jumpFacingRight = true
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
            jumpProgress += deltaMs.toDouble() / ChickenConfig.JUMP_DURATION_MS
            val size = ChickenConfig.SIZE_PX

            if (jumpProgress >= 1.0) {
                isJumping = false
                jumpProgress = 0.0
                x = jumpStartX + (if (jumpFacingRight) ChickenConfig.JUMP_DISTANCE_PX else -ChickenConfig.JUMP_DISTANCE_PX)
                y = jumpStartY
                x = x.coerceIn(0.0, screenWidthPx - size)
                animState = movingAnimState()
            } else {
                // sin(progress*PI) rises from 0 to 1 and back to 0 across the jump,
                // so height eases to zero velocity at the apex (slow at the top,
                // fast at takeoff/landing) rather than moving at a constant rate.
                val height = ChickenConfig.JUMP_HEIGHT_PX * kotlin.math.sin(jumpProgress * Math.PI)
                val horizontalOffset = ChickenConfig.JUMP_DISTANCE_PX * jumpProgress *
                    (if (jumpFacingRight) 1.0 else -1.0)
                x = (jumpStartX + horizontalOffset).coerceIn(0.0, screenWidthPx - size)
                y = (jumpStartY - height).coerceIn(0.0, screenHeightPx - size)
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

        val seconds = deltaMs / 1000.0

        if (rng.nextDouble() < ChickenConfig.DIRECTION_CHANGE_CHANCE * seconds) {
            velocityX = (rng.nextDouble() - 0.5) * ChickenConfig.MAX_VELOCITY
            velocityY = (rng.nextDouble() - 0.5) * ChickenConfig.MAX_VELOCITY
        }

        if (rng.nextDouble() < ChickenConfig.PECK_CHANCE * seconds) {
            isPecking = true
            peckRemainingMs = ChickenConfig.PECK_DURATION_MS
            animState = ChickenAnimState.PECKING
            return
        }

        if (rng.nextDouble() < ChickenConfig.IDLE_CHANCE * seconds) {
            isIdling = true
            idleRemainingMs = ChickenConfig.IDLE_MIN_DURATION_MS +
                (rng.nextDouble() * (ChickenConfig.IDLE_MAX_DURATION_MS - ChickenConfig.IDLE_MIN_DURATION_MS)).toLong()
            animState = ChickenAnimState.IDLE
            return
        }

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
        jumpProgress = 0.0
        jumpStartX = x
        jumpStartY = y
        jumpFacingRight = facingRight
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
