package com.mombotro.chikins.sim

interface Rng {
    fun nextDouble(): Double
    fun nextLongInRange(min: Long, max: Long): Long
}

class RandomRng(private val random: java.util.Random = java.util.Random()) : Rng {
    override fun nextDouble(): Double = random.nextDouble()

    override fun nextLongInRange(min: Long, max: Long): Long {
        require(max >= min)
        if (max == min) return min
        return min + (random.nextDouble() * (max - min)).toLong()
    }
}

class FakeRng(
    private val doubleValue: Double = 0.5,
    private val longValue: Long? = null
) : Rng {
    override fun nextDouble(): Double = doubleValue
    override fun nextLongInRange(min: Long, max: Long): Long = longValue ?: min
}
