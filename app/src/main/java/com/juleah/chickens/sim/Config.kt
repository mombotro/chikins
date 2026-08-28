package com.juleah.chickens.sim

object ChickenConfig {
    const val SIZE_PX = 32

    // Velocity is in px/second (not px/tick) so movement speed is independent
    // of the actual tick rate the device achieves.
    const val MIN_VELOCITY = 15.0
    const val MAX_VELOCITY = 90.0
    const val MOVING_SPEED_THRESHOLD = 5.0
    const val FEED_APPROACH_SPEED = 100.0

    // All *_CHANCE values are probability PER SECOND, applied each tick as
    // chance * (deltaMs / 1000.0). mombotro's original numbers were tuned as
    // a flat per-frame chance against its ~60fps browser rAF loop; our real
    // tick rate varies a lot (roughly 3-6fps on real hardware, since
    // WindowManager view updates dominate tick cost - see OverlayService),
    // so a flat per-roll chance would fire far less often than intended and
    // scale unpredictably with device speed. These are mombotro's per-frame
    // values * 60 to convert them to a rate, so behavior frequency matches
    // mombotro's intent regardless of actual tick rate.
    const val DIRECTION_CHANGE_CHANCE = 0.6
    const val PECK_CHANCE = 0.3
    const val PECK_DURATION_MS = 600L
    const val IDLE_CHANCE = 0.18
    const val IDLE_MIN_DURATION_MS = 1000L
    const val IDLE_MAX_DURATION_MS = 5000L
    const val JUMP_DURATION_MS = 800L
    const val JUMP_HEIGHT_PX = 60.0
    const val JUMP_DISTANCE_PX = 40.0
    const val EGG_LAY_CHANCE = 0.006
    const val EGG_COOLDOWN_MS = 30000L
    const val EGG_SIT_CHANCE = 0.6
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

    // Per-second chances, same rationale as ChickenConfig's *_CHANCE block.
    const val PECK_CHANCE = 0.3
    const val PECK_DURATION_MS = 600L
    const val WANDER_DIRECTION_CHANGE_CHANCE = 0.6

    // Riding on the parent chicken's back, ported from mombotro's chick.js.
    const val RIDING_CHANCE = 0.18
    const val RIDING_STOP_CHANCE = 0.12
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
    const val MIN_SITTING_REQUIRED_MS = 15_000L
    const val MAX_SITTING_REQUIRED_MS = 30_000L

    // Gap between the chicken leaving (sitting timer done) and the hatching
    // animation visibly starting, so the chicken has time to actually walk
    // away first instead of the egg hatching right under it.
    const val MOVE_AWAY_DELAY_MS = 2000L
    // Hatching plays through spritesheet frames 1 (cracked), 2 (peeking out),
    // 3 (broken free) at this interval; the chick is created the moment frame
    // 3 finishes, then frame 4 (empty shell) stays visible for
    // SHELL_VISIBLE_MS before the egg view is removed.
    const val HATCH_FRAME_DELAY_MS = 800L
    const val SHELL_VISIBLE_MS = 2000L

    // Offset from the laying chicken's position - below and to the right,
    // not directly under it.
    const val LAY_OFFSET_X_PX = 12.0
    const val LAY_OFFSET_Y_PX = 24.0
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
