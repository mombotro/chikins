package com.juleah.chickens.sim

enum class PopulationAction { SPAWN_ADULT, SUPPRESS_EGG_LAYING, NORMAL }

object PopulationRegulator {
    fun decide(currentCount: Int): PopulationAction = when {
        currentCount < PopulationConfig.MIN_POPULATION -> PopulationAction.SPAWN_ADULT
        currentCount >= PopulationConfig.MAX_POPULATION -> PopulationAction.SUPPRESS_EGG_LAYING
        else -> PopulationAction.NORMAL
    }
}
