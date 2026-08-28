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

    fun startSitting(chickenId: Long, nowMs: Long) {
        if (state != EggState.WAITING || sittingChickenId != null) return
        sittingChickenId = chickenId
        sittingStartMs = nowMs
        state = EggState.SITTING
    }

    fun sittingChickenIdOrNull(): Long? = sittingChickenId

    fun tick(nowMs: Long) {
        if (state == EggState.SITTING && sittingChickenId != null) {
            if (nowMs - sittingStartMs >= sittingRequiredMs) {
                state = EggState.HATCHING
            }
        }
    }

    fun hatch(newChickId: Long, nowMs: Long): ChickEntity {
        state = EggState.HATCHED
        return ChickEntity(id = newChickId, x = x, y = y, parentId = laidByChickenId, hatchTimeMs = nowMs, rng = rng)
    }
}
