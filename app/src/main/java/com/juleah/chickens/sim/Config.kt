package com.juleah.chickens.sim

object ChickenConfig {
    const val SIZE_PX = 32

    // Velocity is in px/second (not px/tick) so movement speed is independent
    // of the actual tick rate the device achieves.
    const val MIN_VELOCITY = 15.0
    const val MAX_VELOCITY = 90.0
    const val MOVING_SPEED_THRESHOLD = 5.0
    const val FEED_APPROACH_SPEED = 100.0

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

    // Frame indices and per-frame display duration for each animation state,
    // matching the chicken.png spritesheet layout: 0-1 idle, 2-5 walk, 6-7
    // peck, 8-9 jump, 10 nesting/sitting.
    val IDLE_FRAMES = intArrayOf(0, 1)
    const val IDLE_FRAME_SPEED_MS = 500L
    val WALKING_FRAMES = intArrayOf(2, 3, 4, 5)
    const val WALKING_FRAME_SPEED_MS = 150L
    val PECKING_FRAMES = intArrayOf(6, 7)
    const val PECKING_FRAME_SPEED_MS = 300L
    val JUMPING_FRAMES = intArrayOf(8, 9)
    const val JUMPING_FRAME_SPEED_MS = 100L
    val SITTING_FRAMES = intArrayOf(10)
    const val SITTING_FRAME_SPEED_MS = 1000L
}

object ChickConfig {
    const val SIZE_PX = 16
    const val MIN_GROWTH_MS = 1 * 60_000L
    const val MAX_GROWTH_MS = 3 * 60_000L

    // px/second, same rationale as ChickenConfig.
    const val FOLLOW_PARENT_SPEED = 70.0
    const val WANDER_SPEED = 40.0
    const val RUN_AWAY_SPEED = 150.0
    const val RUN_AWAY_DURATION_MS = 3000L
    const val FOLLOW_PARENT_DISTANCE_PX = 40.0
    const val MOVING_SPEED_THRESHOLD = 5.0

    const val PECK_CHANCE = 0.005
    const val PECK_DURATION_MS = 600L

    // Riding on the parent chicken's back, ported from mombotro's chick.js.
    const val RIDING_CHANCE = 0.003
    const val RIDING_STOP_CHANCE = 0.002
    const val RIDING_MIN_DURATION_MS = 1500L
    const val RIDING_MAX_DURATION_MS = 3500L
    const val RIDING_TRIGGER_DISTANCE_PX = 15.0

    // Frame indices/speeds matching chick.png: 0-1 idle, 2-3 walk, 4-5 peck.
    // Riding reuses idle frame 1 (sitting still), same as mombotro.
    val IDLE_FRAMES = intArrayOf(0, 1)
    const val IDLE_FRAME_SPEED_MS = 500L
    val WALKING_FRAMES = intArrayOf(2, 3)
    const val WALKING_FRAME_SPEED_MS = 150L
    val RUNNING_FRAMES = intArrayOf(2, 3)
    const val RUNNING_FRAME_SPEED_MS = 100L
    val PECKING_FRAMES = intArrayOf(4, 5)
    const val PECKING_FRAME_SPEED_MS = 300L
    val RIDING_FRAMES = intArrayOf(1)
    const val RIDING_FRAME_SPEED_MS = 1000L
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
