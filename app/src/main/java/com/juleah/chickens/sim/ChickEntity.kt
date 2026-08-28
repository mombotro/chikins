package com.juleah.chickens.sim

enum class ChickAnimState { IDLE, WALKING, RUNNING }

class ChickEntity(
    val id: Long,
    var x: Double,
    var y: Double,
    val parentId: Long?,
    hatchTimeMs: Long,
    private val rng: Rng
) {
    val growUpAtMs: Long = hatchTimeMs + rng.nextLongInRange(ChickConfig.MIN_GROWTH_MS, ChickConfig.MAX_GROWTH_MS)
    var hasGrownUp: Boolean = false
        private set

    var animState: ChickAnimState = ChickAnimState.IDLE
        private set
    var facingRight: Boolean = true
        private set

    private var velocityX: Double = 0.0
    private var velocityY: Double = 0.0
    private var isRunningAway = false
    private var runAwayRemainingMs = 0L

    private var lastAnimState = ChickAnimState.IDLE
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

    fun runAway() {
        isRunningAway = true
        runAwayRemainingMs = ChickConfig.RUN_AWAY_DURATION_MS
    }

    fun followParent(deltaMs: Long, parentX: Double, parentY: Double, screenWidthPx: Double, screenHeightPx: Double) {
        advanceAnimation(deltaMs)

        if (isRunningAway) {
            runFree(deltaMs, screenWidthPx, screenHeightPx)
            return
        }

        val dx = parentX - x
        val dy = parentY - y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

        if (distance <= ChickConfig.FOLLOW_PARENT_DISTANCE_PX) {
            animState = ChickAnimState.IDLE
            return
        }

        velocityX = (dx / distance) * ChickConfig.FOLLOW_PARENT_SPEED
        velocityY = (dy / distance) * ChickConfig.FOLLOW_PARENT_SPEED
        move(deltaMs, screenWidthPx, screenHeightPx)
        animState = ChickAnimState.WALKING
    }

    fun wanderAlone(deltaMs: Long, screenWidthPx: Double, screenHeightPx: Double) {
        advanceAnimation(deltaMs)

        if (isRunningAway) {
            runFree(deltaMs, screenWidthPx, screenHeightPx)
            return
        }

        if (rng.nextDouble() < 0.01) {
            velocityX = (rng.nextDouble() - 0.5) * ChickConfig.WANDER_SPEED
            velocityY = (rng.nextDouble() - 0.5) * ChickConfig.WANDER_SPEED
        }
        move(deltaMs, screenWidthPx, screenHeightPx)
        animState = if (kotlin.math.abs(velocityX) + kotlin.math.abs(velocityY) > ChickConfig.MOVING_SPEED_THRESHOLD) {
            ChickAnimState.WALKING
        } else {
            ChickAnimState.IDLE
        }
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

    private fun framesFor(state: ChickAnimState): IntArray = when (state) {
        ChickAnimState.IDLE -> ChickConfig.IDLE_FRAMES
        ChickAnimState.WALKING -> ChickConfig.WALKING_FRAMES
        ChickAnimState.RUNNING -> ChickConfig.RUNNING_FRAMES
    }

    private fun speedFor(state: ChickAnimState): Long = when (state) {
        ChickAnimState.IDLE -> ChickConfig.IDLE_FRAME_SPEED_MS
        ChickAnimState.WALKING -> ChickConfig.WALKING_FRAME_SPEED_MS
        ChickAnimState.RUNNING -> ChickConfig.RUNNING_FRAME_SPEED_MS
    }
}
