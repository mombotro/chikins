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

    fun tick(nowMs: Long) {
        if (nowMs >= growUpAtMs) {
            hasGrownUp = true
        }
    }

    fun toChicken(nowMs: Long, rng: Rng): ChickenEntity =
        ChickenEntity(id = id, x = x, y = y, birthTimeMs = nowMs, rng = rng)
}
