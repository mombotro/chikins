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

    fun eggsReadyToHatch(): List<EggEntity> = eggs.filter { it.state == EggState.HATCHING }

    fun confirmHatch(eggId: Long) {
        val egg = eggs.find { it.id == eggId } ?: return
        if (egg.state != EggState.HATCHING) return
        chicks.add(egg.hatch(newChickId = newId(), nowMs = clock.nowMs()))
        eggs.removeAll { it.id == eggId }
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

        if (PopulationRegulator.decide(populationCount()) == PopulationAction.SPAWN_ADULT) {
            spawnAdultChicken()
        }
    }
}
