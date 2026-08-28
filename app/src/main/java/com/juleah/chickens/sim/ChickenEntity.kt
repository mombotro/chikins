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

    fun tick(nowMs: Long) {
        if (nowMs >= deathAtMs) {
            isDead = true
        }
    }
}
