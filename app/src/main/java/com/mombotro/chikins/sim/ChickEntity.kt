package com.mombotro.chikins.sim

enum class ChickAnimState { IDLE, WALKING, RUNNING, PECKING, RIDING }

class ChickEntity(
    val id: Long,
    var x: Double,
    var y: Double,
    val parentId: Long?,
    hatchTimeMs: Long,
    private val rng: Rng
) {
    var growUpAtMs: Long = hatchTimeMs + rng.nextLongInRange(ChickConfig.MIN_GROWTH_MS, ChickConfig.MAX_GROWTH_MS)
        private set
    var hasGrownUp: Boolean = false
        private set

    private var feedEatCooldownRemainingMs = 0L
    private var targetFeedId: Long? = null

    /**
     * Resets the frame index/timer the instant the state actually changes,
     * not on the next advanceAnimation() call. Without this, a method that
     * sets animState after advanceAnimation() already ran this tick (e.g.
     * the peck-chance branch in wanderAlone()) would render one frame
     * picked from the *previous* state's frame count against the *new*
     * state's array - a visible one-tick glitch between e.g. walking and
     * pecking.
     */
    var animState: ChickAnimState = ChickAnimState.IDLE
        private set(value) {
            if (field != value) {
                animFrameIndex = 0
                animFrameTimeMs = 0L
            }
            field = value
        }
    var facingRight: Boolean = true
        private set

    private var velocityX: Double = 0.0
    private var velocityY: Double = 0.0
    private var isRunningAway = false
    private var runAwayRemainingMs = 0L
    private var isPecking = false
    private var peckRemainingMs = 0L
    private var isRiding = false
    private var ridingRemainingMs = 0L

    private var animFrameIndex = 0
    private var animFrameTimeMs = 0L

    fun tick(nowMs: Long) {
        if (nowMs >= growUpAtMs) {
            hasGrownUp = true
        }
    }

    fun toChicken(nowMs: Long, rng: Rng): ChickenEntity =
        ChickenEntity(id = id, x = x, y = y, birthTimeMs = nowMs, rng = rng)

    fun currentSpriteFrame(): Int {
        val frames = framesFor(animState)
        return frames[animFrameIndex % frames.size]
    }

    fun isBusyRiding(): Boolean = isRiding

    /** Tap handling: dismount if riding, otherwise run away, matching mombotro. */
    fun handleTap() {
        if (isRiding) {
            isRiding = false
            animState = ChickAnimState.IDLE
        } else {
            runAway()
        }
    }

    fun runAway() {
        isRunningAway = true
        runAwayRemainingMs = ChickConfig.RUN_AWAY_DURATION_MS
    }

    fun currentTargetFeedId(): Long? = targetFeedId
    fun clearTargetFeed() { targetFeedId = null }
    fun rushToFeed(feedId: Long) { targetFeedId = feedId }

    /** Whether this chick can eat from a feed pile right now (see FEED_EAT_COOLDOWN_MS). */
    fun canEatFeed(): Boolean = feedEatCooldownRemainingMs <= 0

    /** Eating knocks time off growUpAtMs directly - eating makes chicks grow up faster. */
    fun eatFeed() {
        growUpAtMs -= ChickConfig.GROWTH_REDUCTION_PER_FEED_MS
        feedEatCooldownRemainingMs = ChickConfig.FEED_EAT_COOLDOWN_MS
    }

    private fun tickFeedCooldown(deltaMs: Long) {
        if (feedEatCooldownRemainingMs > 0) feedEatCooldownRemainingMs -= deltaMs
    }

    /** Called when this chick has a feed pile target (see rushToFeed). Returns true once it actually takes a bite. */
    fun moveTowardFeed(deltaMs: Long, feedX: Double, feedY: Double, screenWidthPx: Double, screenHeightPx: Double): Boolean {
        advanceAnimation(deltaMs)
        tickFeedCooldown(deltaMs)

        val centeringOffset = (ChickConfig.SIZE_PX - FeedConfig.SIZE_PX) / 2.0
        val targetX = feedX - centeringOffset
        val targetY = feedY - centeringOffset
        val dx = targetX - x
        val dy = targetY - y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

        if (distance <= 10.0) {
            animState = ChickAnimState.PECKING
            if (canEatFeed()) {
                eatFeed()
                return true
            }
            return false
        }

        val seconds = deltaMs / 1000.0
        x += (dx / distance) * ChickConfig.FEED_APPROACH_SPEED * seconds
        y += (dy / distance) * ChickConfig.FEED_APPROACH_SPEED * seconds
        facingRight = dx > 0
        animState = ChickAnimState.WALKING
        return false
    }

    fun followParent(
        deltaMs: Long,
        parentX: Double,
        parentY: Double,
        parentFacingRight: Boolean,
        screenWidthPx: Double,
        screenHeightPx: Double
    ) {
        advanceAnimation(deltaMs)
        tickFeedCooldown(deltaMs)

        if (isRunningAway) {
            runFree(deltaMs, screenWidthPx, screenHeightPx)
            return
        }

        val seconds = deltaMs / 1000.0

        if (isRiding) {
            ridingRemainingMs -= deltaMs
            if (ridingRemainingMs <= 0 || rng.nextDouble() < ChickConfig.RIDING_STOP_CHANCE * seconds) {
                stopRiding(screenWidthPx, screenHeightPx)
            } else {
                x = parentX + 2.0
                y = parentY - 8.0
                facingRight = parentFacingRight
                animState = ChickAnimState.RIDING
                return
            }
        }

        val dx = parentX - x
        val dy = parentY - y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

        if (distance <= ChickConfig.RIDING_TRIGGER_DISTANCE_PX && rng.nextDouble() < ChickConfig.RIDING_CHANCE * seconds) {
            isRiding = true
            ridingRemainingMs = ChickConfig.RIDING_MIN_DURATION_MS +
                (rng.nextDouble() * (ChickConfig.RIDING_MAX_DURATION_MS - ChickConfig.RIDING_MIN_DURATION_MS)).toLong()
            animState = ChickAnimState.RIDING
            return
        }

        if (distance <= ChickConfig.FOLLOW_PARENT_DISTANCE_PX) {
            // Close enough: mill around near the parent instead of freezing
            // in place. Wandering back out past FOLLOW_PARENT_DISTANCE_PX
            // just re-triggers the walk-toward-parent branch above on a
            // later tick, so no separate "stay within radius" logic is
            // needed here.
            if (rng.nextDouble() < ChickConfig.WANDER_DIRECTION_CHANGE_CHANCE * seconds) {
                velocityX = (rng.nextDouble() - 0.5) * ChickConfig.WANDER_SPEED
                velocityY = (rng.nextDouble() - 0.5) * ChickConfig.WANDER_SPEED
            }
            move(deltaMs, screenWidthPx, screenHeightPx)
            animState = movingAnimState()
            return
        }

        velocityX = (dx / distance) * ChickConfig.FOLLOW_PARENT_SPEED
        velocityY = (dy / distance) * ChickConfig.FOLLOW_PARENT_SPEED
        move(deltaMs, screenWidthPx, screenHeightPx)
        animState = ChickAnimState.WALKING
    }

    fun wanderAlone(deltaMs: Long, screenWidthPx: Double, screenHeightPx: Double) {
        advanceAnimation(deltaMs)
        tickFeedCooldown(deltaMs)

        if (isRunningAway) {
            runFree(deltaMs, screenWidthPx, screenHeightPx)
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

        val seconds = deltaMs / 1000.0

        if (rng.nextDouble() < ChickConfig.PECK_CHANCE * seconds) {
            isPecking = true
            peckRemainingMs = ChickConfig.PECK_DURATION_MS
            animState = ChickAnimState.PECKING
            return
        }

        if (rng.nextDouble() < ChickConfig.WANDER_DIRECTION_CHANGE_CHANCE * seconds) {
            velocityX = (rng.nextDouble() - 0.5) * ChickConfig.WANDER_SPEED
            velocityY = (rng.nextDouble() - 0.5) * ChickConfig.WANDER_SPEED
        }
        move(deltaMs, screenWidthPx, screenHeightPx)
        animState = movingAnimState()
    }

    private fun stopRiding(screenWidthPx: Double, screenHeightPx: Double) {
        isRiding = false
        x += (rng.nextDouble() - 0.5) * 20.0
        y += (rng.nextDouble() - 0.5) * 20.0
        val size = ChickConfig.SIZE_PX
        x = x.coerceIn(0.0, screenWidthPx - size)
        y = y.coerceIn(0.0, screenHeightPx - size)
        animState = ChickAnimState.IDLE
    }

    private fun runFree(deltaMs: Long, screenWidthPx: Double, screenHeightPx: Double) {
        runAwayRemainingMs -= deltaMs
        if (runAwayRemainingMs <= 0) {
            isRunningAway = false
        }
        if (kotlin.math.abs(velocityX) + kotlin.math.abs(velocityY) < 1.0) {
            velocityX = (rng.nextDouble() - 0.5) * ChickConfig.RUN_AWAY_SPEED
            velocityY = (rng.nextDouble() - 0.5) * ChickConfig.RUN_AWAY_SPEED
        } else {
            val magnitude = kotlin.math.sqrt(velocityX * velocityX + velocityY * velocityY)
            velocityX = (velocityX / magnitude) * ChickConfig.RUN_AWAY_SPEED
            velocityY = (velocityY / magnitude) * ChickConfig.RUN_AWAY_SPEED
        }
        move(deltaMs, screenWidthPx, screenHeightPx)
        animState = ChickAnimState.RUNNING
    }

    private fun move(deltaMs: Long, screenWidthPx: Double, screenHeightPx: Double) {
        val seconds = deltaMs / 1000.0
        x += velocityX * seconds
        y += velocityY * seconds
        facingRight = velocityX > 0.1

        val size = ChickConfig.SIZE_PX
        if (x <= 0 || x >= screenWidthPx - size) {
            velocityX = -velocityX
            x = x.coerceIn(0.0, screenWidthPx - size)
        }
        if (y <= 0 || y >= screenHeightPx - size) {
            velocityY = -velocityY
            y = y.coerceIn(0.0, screenHeightPx - size)
        }
    }

    private fun movingAnimState(): ChickAnimState =
        if (kotlin.math.abs(velocityX) + kotlin.math.abs(velocityY) > ChickConfig.MOVING_SPEED_THRESHOLD) {
            ChickAnimState.WALKING
        } else {
            ChickAnimState.IDLE
        }

    private fun advanceAnimation(deltaMs: Long) {
        animFrameTimeMs += deltaMs
        val speed = speedFor(animState)
        if (animFrameTimeMs >= speed) {
            animFrameTimeMs = 0L
            val frames = framesFor(animState)
            animFrameIndex = (animFrameIndex + 1) % frames.size
        }
    }

    private fun framesFor(state: ChickAnimState): IntArray = when (state) {
        ChickAnimState.IDLE -> ChickConfig.IDLE_FRAMES
        ChickAnimState.WALKING -> ChickConfig.WALKING_FRAMES
        ChickAnimState.RUNNING -> ChickConfig.RUNNING_FRAMES
        ChickAnimState.PECKING -> ChickConfig.PECKING_FRAMES
        ChickAnimState.RIDING -> ChickConfig.RIDING_FRAMES
    }

    private fun speedFor(state: ChickAnimState): Long = when (state) {
        ChickAnimState.IDLE -> ChickConfig.IDLE_FRAME_SPEED_MS
        ChickAnimState.WALKING -> ChickConfig.WALKING_FRAME_SPEED_MS
        ChickAnimState.RUNNING -> ChickConfig.RUNNING_FRAME_SPEED_MS
        ChickAnimState.PECKING -> ChickConfig.PECKING_FRAME_SPEED_MS
        ChickAnimState.RIDING -> ChickConfig.RIDING_FRAME_SPEED_MS
    }
}
