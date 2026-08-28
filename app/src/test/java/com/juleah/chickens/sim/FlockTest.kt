package com.juleah.chickens.sim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlockTest {

    private fun newFlock(clock: Clock, rng: Rng = FakeRng(longValue = ChickenConfig.MIN_LIFESPAN_MS)) =
        Flock(clock = clock, rng = rng, screenWidthPx = 1080.0, screenHeightPx = 1920.0)

    @Test
    fun `seedInitialPopulation creates the configured starting count`() {
        val flock = newFlock(FakeClock(0L))
        flock.seedInitialPopulation()
        assertEquals(PopulationConfig.INITIAL_POPULATION, flock.populationCount())
    }

    @Test
    fun `tick below minimum population spawns a replacement adult`() {
        val flock = newFlock(FakeClock(0L))
        // starts empty: below MIN_POPULATION

        flock.tick()

        assertEquals(1, flock.populationCount())
    }

    @Test
    fun `population at maximum blocks egg laying`() {
        val flock = newFlock(FakeClock(0L))
        repeat(PopulationConfig.MAX_POPULATION) { flock.spawnAdultChicken() }

        assertFalse(flock.canLayEgg())
    }

    @Test
    fun `population in normal band allows egg laying`() {
        val flock = newFlock(FakeClock(0L))
        repeat(PopulationConfig.MIN_POPULATION) { flock.spawnAdultChicken() }

        assertTrue(flock.canLayEgg())
    }

    @Test
    fun `population regenerates after simultaneous deaths`() {
        val clock = FakeClock(0L)
        val rng = FakeRng(longValue = ChickenConfig.MIN_LIFESPAN_MS)
        val flock = newFlock(clock, rng)
        repeat(PopulationConfig.MIN_POPULATION) { flock.spawnAdultChicken() }

        clock.set(ChickenConfig.MIN_LIFESPAN_MS)
        flock.tick()

        assertEquals(1, flock.populationCount())
    }

    @Test
    fun `chick converts to chicken after growth time`() {
        val clock = FakeClock(0L)
        val growthRng = FakeRng(longValue = ChickConfig.MIN_GROWTH_MS)
        val flock = newFlock(clock, growthRng)
        val chick = ChickEntity(id = 99, x = 5.0, y = 5.0, parentId = null, hatchTimeMs = 0L, rng = growthRng)
        flock.chicks.add(chick)

        clock.set(ChickConfig.MIN_GROWTH_MS)
        flock.tick()

        assertTrue(flock.chicks.isEmpty())
        assertTrue(flock.chickens.any { it.id == 99L })
    }

    @Test
    fun `egg reaches hatching state and confirmHatch produces a chick`() {
        val clock = FakeClock(0L)
        val rng = FakeRng(longValue = EggConfig.MIN_SITTING_REQUIRED_MS)
        val flock = newFlock(clock, rng)
        val egg = flock.layEgg(chickenId = 1, x = 10.0, y = 10.0)
        egg.startSitting(chickenId = 1, nowMs = 0L)

        clock.set(EggConfig.MIN_SITTING_REQUIRED_MS)
        flock.tick()

        assertEquals(1, flock.eggsReadyToHatch().size)

        flock.confirmHatch(egg.id)

        assertTrue(flock.eggs.isEmpty())
        assertEquals(1, flock.chicks.size)
    }
}
