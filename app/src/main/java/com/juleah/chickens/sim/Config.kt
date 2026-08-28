package com.juleah.chickens.sim

object ChickenConfig {
    const val SIZE_PX = 32
    const val MIN_VELOCITY = 0.5
    const val MAX_VELOCITY = 2.0
    const val DIRECTION_CHANGE_CHANCE = 0.01
    const val PECK_CHANCE = 0.005
    const val PECK_DURATION_MS = 600L
    const val IDLE_CHANCE = 0.003
    const val IDLE_MIN_DURATION_MS = 1000L
    const val IDLE_MAX_DURATION_MS = 5000L
    const val JUMP_DURATION_MS = 800L
    const val EGG_LAY_CHANCE = 0.0001
    const val EGG_COOLDOWN_MS = 30000L
    const val EGG_SIT_CHANCE = 0.01
    const val EGG_SIT_DISTANCE_PX = 30.0
    const val MIN_LIFESPAN_MS = 5 * 60_000L
    const val MAX_LIFESPAN_MS = 15 * 60_000L
}

object ChickConfig {
    const val SIZE_PX = 16
    const val MIN_GROWTH_MS = 1 * 60_000L
    const val MAX_GROWTH_MS = 3 * 60_000L
}

object EggConfig {
    const val SIZE_PX = 16
    const val MIN_SITTING_REQUIRED_MS = 45_000L
    const val MAX_SITTING_REQUIRED_MS = 90_000L
}

object FeedConfig {
    const val SIZE_PX = 16
    const val INITIAL_AMOUNT = 100
    const val CONSUME_AMOUNT = 10
    const val NOTIFY_RADIUS_FRACTION = 0.5
    const val FULL_FRAME_THRESHOLD = 0.66
    const val HALF_FRAME_THRESHOLD = 0.33
}

object PopulationConfig {
    const val MIN_POPULATION = 5
    const val MAX_POPULATION = 15
    const val INITIAL_POPULATION = 8
}
