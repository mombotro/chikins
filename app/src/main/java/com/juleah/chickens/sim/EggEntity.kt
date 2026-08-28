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

    /** True once the HATCHING animation's own duration has elapsed - only then is confirmHatch() meaningful. */
    var isReadyToHatch: Boolean = false
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

        if (state == EggState.HATCHING && !isReadyToHatch) {
            if (nowMs - hatchStartedAtMs >= EggConfig.HATCH_DURATION_MS) {
                isReadyToHatch = true
            }
        }
    }

    fun hatch(newChickId: Long, nowMs: Long): ChickEntity {
        state = EggState.HATCHED
        return ChickEntity(id = newChickId, x = x, y = y, parentId = laidByChickenId, hatchTimeMs = nowMs, rng = rng)
    }
}
