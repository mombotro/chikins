package com.juleah.chickens.sim

enum class EggState { WAITING, SITTING, HATCHING, HATCHED }

class EggEntity(
    val id: Long,
    val x: Double,
    val y: Double,
    val laidByChickenId: Long,
    private val rng: Rng
) {
    var state: EggState = EggState.WAITING
        private set

    private var sittingChickenId: Long? = null
    private var sittingStartMs: Long = 0L
    private val sittingRequiredMs: Long =
        rng.nextLongInRange(EggConfig.MIN_SITTING_REQUIRED_MS, EggConfig.MAX_SITTING_REQUIRED_MS)

    // Set the moment sitting completes: the chicken is released to walk away
    // immediately, but the egg doesn't visibly start hatching until
    // MOVE_AWAY_DELAY_MS later, giving it time to actually leave first.
    private var releasedAtMs: Long? = null
    private var hatchStartedAtMs: Long = 0L
    private var shellRemoveAtMs: Long = 0L

    /** True once frames 1-3 (crack/peek/break-free) have finished playing - the chick should be created now. */
    var isReadyToHatch: Boolean = false
        private set

    /** True once hatch() has been called - guards against creating the chick twice while the shell lingers. */
    var hatchConfirmed: Boolean = false
        private set

    /** True once the empty-shell frame has been visible for SHELL_VISIBLE_MS - the egg view can be removed. */
    var isReadyToRemove: Boolean = false
        private set

    fun startSitting(chickenId: Long, nowMs: Long) {
        if (state != EggState.WAITING || sittingChickenId != null) return
        sittingChickenId = chickenId
        sittingStartMs = nowMs
        state = EggState.SITTING
    }

    fun sittingChickenIdOrNull(): Long? = sittingChickenId

    fun tick(nowMs: Long) {
        if (state == EggState.SITTING) {
            if (sittingChickenId != null) {
                if (nowMs - sittingStartMs >= sittingRequiredMs) {
                    sittingChickenId = null
                    releasedAtMs = nowMs
                }
                return
            }
            val released = releasedAtMs ?: return
            if (nowMs - released >= EggConfig.MOVE_AWAY_DELAY_MS) {
                state = EggState.HATCHING
                hatchStartedAtMs = nowMs
            }
            return
        }

        if (state != EggState.HATCHING) return

        if (!isReadyToHatch) {
            if (nowMs - hatchStartedAtMs >= EggConfig.HATCH_FRAME_DELAY_MS * 3) {
                isReadyToHatch = true
            }
            return
        }

        if (hatchConfirmed && !isReadyToRemove && nowMs >= shellRemoveAtMs) {
            isReadyToRemove = true
            state = EggState.HATCHED
        }
    }

    /** Sprite-sheet frame (0-4) for the egg's current visible state. */
    fun currentSpriteFrame(nowMs: Long): Int {
        if (state != EggState.HATCHING) return 0
        if (!isReadyToHatch) {
            val elapsed = nowMs - hatchStartedAtMs
            val crackIndex = (elapsed / EggConfig.HATCH_FRAME_DELAY_MS).toInt().coerceIn(0, 2)
            return crackIndex + 1 // frames 1 (cracked), 2 (peeking), 3 (broken free)
        }
        return 4 // empty shell
    }

    fun hatch(newChickId: Long, nowMs: Long): ChickEntity {
        hatchConfirmed = true
        shellRemoveAtMs = nowMs + EggConfig.SHELL_VISIBLE_MS
        return ChickEntity(id = newChickId, x = x, y = y, parentId = laidByChickenId, hatchTimeMs = nowMs, rng = rng)
    }
}
