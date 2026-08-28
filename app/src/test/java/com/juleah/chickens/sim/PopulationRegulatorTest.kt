package com.juleah.chickens.sim

import org.junit.Assert.assertEquals
import org.junit.Test

class PopulationRegulatorTest {

    @Test
    fun `below minimum triggers direct adult spawn`() {
        assertEquals(PopulationAction.SPAWN_ADULT, PopulationRegulator.decide(currentCount = 4))
    }

    @Test
    fun `at minimum is normal band`() {
        assertEquals(PopulationAction.NORMAL, PopulationRegulator.decide(currentCount = PopulationConfig.MIN_POPULATION))
    }

    @Test
    fun `at maximum suppresses egg laying`() {
        assertEquals(PopulationAction.SUPPRESS_EGG_LAYING, PopulationRegulator.decide(currentCount = PopulationConfig.MAX_POPULATION))
    }

    @Test
    fun `above maximum suppresses egg laying`() {
        assertEquals(PopulationAction.SUPPRESS_EGG_LAYING, PopulationRegulator.decide(currentCount = PopulationConfig.MAX_POPULATION + 3))
    }

    @Test
    fun `mid band is normal`() {
        val midpoint = (PopulationConfig.MIN_POPULATION + PopulationConfig.MAX_POPULATION) / 2
        assertEquals(PopulationAction.NORMAL, PopulationRegulator.decide(currentCount = midpoint))
    }
}
