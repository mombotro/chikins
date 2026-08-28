package com.juleah.chickens.sim

class Flock(
    private val clock: Clock,
    private val rng: Rng,
    private val screenWidthPx: Double,
    private val screenHeightPx: Double
) {
    val chickens: MutableList<ChickenEntity> = mutableListOf()
    val chicks: MutableList<ChickEntity> = mutableListOf()
    val eggs: MutableList<EggEntity> = mutableListOf()

    private var nextId: Long = 0L
    private fun newId(): Long = nextId++

    private val lastEggTimeByChickenId = mutableMapOf<Long, Long>()

    fun populationCount(): Int = chickens.size + chicks.size

    fun canLayEgg(): Boolean =
        PopulationRegulator.decide(populationCount()) != PopulationAction.SUPPRESS_EGG_LAYING

    fun seedInitialPopulation() {
        repeat(PopulationConfig.INITIAL_POPULATION) { spawnAdultChicken() }
    }

    fun spawnAdultChicken(): ChickenEntity {
        val chicken = ChickenEntity(
            id = newId(),
            x = rng.nextDouble() * screenWidthPx,
            y = rng.nextDouble() * screenHeightPx,
            birthTimeMs = clock.nowMs(),
            rng = rng
        )
        chickens.add(chicken)
        return chicken
    }

    fun layEgg(chickenId: Long, x: Double, y: Double): EggEntity {
        val egg = EggEntity(id = newId(), x = x, y = y, laidByChickenId = chickenId, rng = rng)
        eggs.add(egg)
        return egg
    }

    /**
     * deltaMs scales the chance the same way ChickenEntity.wander() scales its
     * own per-second chances - see the comment on ChickenConfig's *_CHANCE
     * block for why a flat per-roll chance doesn't work with a variable tick
     * rate.
     */
    fun maybeLayEgg(chicken: ChickenEntity, deltaMs: Long): EggEntity? {
        if (!canLayEgg()) return null
        val now = clock.nowMs()
        val last = lastEggTimeByChickenId[chicken.id] ?: 0L
        if (now - last < ChickenConfig.EGG_COOLDOWN_MS) return null
        if (rng.nextDouble() >= ChickenConfig.EGG_LAY_CHANCE * (deltaMs / 1000.0)) return null
        lastEggTimeByChickenId[chicken.id] = now
        return layEgg(chicken.id, chicken.x + EggConfig.LAY_OFFSET_X_PX, chicken.y + EggConfig.LAY_OFFSET_Y_PX)
    }

    fun maybeSitOnEgg(chicken: ChickenEntity, deltaMs: Long): EggEntity? {
        val candidate = eggs.find { egg ->
            egg.state == EggState.WAITING &&
                egg.sittingChickenIdOrNull() == null &&
                distanceBetween(chicken.x, chicken.y, egg.x, egg.y) <= ChickenConfig.EGG_SIT_DISTANCE_PX
        } ?: return null
        if (rng.nextDouble() >= ChickenConfig.EGG_SIT_CHANCE * (deltaMs / 1000.0)) return null
        candidate.startSitting(chicken.id, clock.nowMs())
        return candidate
    }

    private fun distanceBetween(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val dx = x1 - x2
        val dy = y1 - y2
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    fun eggsReadyToHatch(): List<EggEntity> =
        eggs.filter { it.state == EggState.HATCHING && it.isReadyToHatch && !it.hatchConfirmed }

    /** Creates the chick; the egg's empty-shell view lingers until its own SHELL_VISIBLE_MS elapses (see tick()). */
    fun confirmHatch(eggId: Long) {
        val egg = eggs.find { it.id == eggId } ?: return
        if (egg.state != EggState.HATCHING || !egg.isReadyToHatch || egg.hatchConfirmed) return
        chicks.add(egg.hatch(newChickId = newId(), nowMs = clock.nowMs()))
    }

    fun tick() {
        val now = clock.nowMs()

        chickens.forEach { it.tick(now) }
        chickens.removeAll { it.isDead }

        chicks.forEach { it.tick(now) }
        val grown = chicks.filter { it.hasGrownUp }
        chicks.removeAll { it.hasGrownUp }
        grown.forEach { chickens.add(it.toChicken(now, rng)) }

        eggs.forEach { it.tick(now) }
        eggs.removeAll { it.isReadyToRemove }

        if (PopulationRegulator.decide(populationCount()) == PopulationAction.SPAWN_ADULT) {
            spawnAdultChicken()
        }
    }
}
