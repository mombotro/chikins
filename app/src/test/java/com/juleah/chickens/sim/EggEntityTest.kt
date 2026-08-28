package com.juleah.chickens.sim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EggEntityTest {

    private val crackDurationMs = EggConfig.HATCH_FRAME_DELAY_MS * 3

    @Test
    fun `starts in waiting state`() {
        val egg = EggEntity(id = 1, x = 0.0, y = 0.0, laidByChickenId = 5, rng = RandomRng())
        assertEquals(EggState.WAITING, egg.state)
    }

    @Test
    fun `startSitting moves egg into sitting state`() {
        val egg = EggEntity(id = 1, x = 0.0, y = 0.0, laidByChickenId = 5, rng = RandomRng())
        egg.startSitting(chickenId = 5, nowMs = 0L)
        assertEquals(EggState.SITTING, egg.state)
        assertEquals(5L, egg.sittingChickenIdOrNull())
    }

    @Test
    fun `stays sitting before its sitting time elapses`() {
        val rng = FakeRng(longValue = EggConfig.MIN_SITTING_REQUIRED_MS)
        val egg = EggEntity(id = 1, x = 0.0, y = 0.0, laidByChickenId = 5, rng = rng)
        egg.startSitting(chickenId = 5, nowMs = 0L)

        egg.tick(nowMs = EggConfig.MIN_SITTING_REQUIRED_MS - 1)

        assertEquals(EggState.SITTING, egg.state)
    }

    @Test
    fun `releases the sitting chicken once sitting time elapses, but does not hatch yet`() {
        val rng = FakeRng(longValue = EggConfig.MIN_SITTING_REQUIRED_MS)
        val egg = EggEntity(id = 1, x = 0.0, y = 0.0, laidByChickenId = 5, rng = rng)
        egg.startSitting(chickenId = 5, nowMs = 0L)

        egg.tick(nowMs = EggConfig.MIN_SITTING_REQUIRED_MS)

        assertNull(egg.sittingChickenIdOrNull())
        assertEquals(EggState.SITTING, egg.state)
    }

    @Test
    fun `moves to hatching only after the move-away delay elapses`() {
        val rng = FakeRng(longValue = EggConfig.MIN_SITTING_REQUIRED_MS)
        val egg = EggEntity(id = 1, x = 0.0, y = 0.0, laidByChickenId = 5, rng = rng)
        egg.startSitting(chickenId = 5, nowMs = 0L)
        egg.tick(nowMs = EggConfig.MIN_SITTING_REQUIRED_MS)

        egg.tick(nowMs = EggConfig.MIN_SITTING_REQUIRED_MS + EggConfig.MOVE_AWAY_DELAY_MS - 1)
        assertEquals(EggState.SITTING, egg.state)

        egg.tick(nowMs = EggConfig.MIN_SITTING_REQUIRED_MS + EggConfig.MOVE_AWAY_DELAY_MS)
        assertEquals(EggState.HATCHING, egg.state)
        assertFalse(egg.isReadyToHatch)
    }

    private fun eggAtHatchStart(): Pair<EggEntity, Long> {
        val rng = FakeRng(longValue = EggConfig.MIN_SITTING_REQUIRED_MS)
        val egg = EggEntity(id = 1, x = 12.0, y = 34.0, laidByChickenId = 5, rng = rng)
        egg.startSitting(chickenId = 5, nowMs = 0L)
        val hatchStartMs = EggConfig.MIN_SITTING_REQUIRED_MS + EggConfig.MOVE_AWAY_DELAY_MS
        egg.tick(nowMs = EggConfig.MIN_SITTING_REQUIRED_MS)
        egg.tick(nowMs = hatchStartMs)
        return egg to hatchStartMs
    }

    @Test
    fun `plays through crack frames 1-3 before becoming ready to hatch`() {
        val (egg, hatchStartMs) = eggAtHatchStart()

        assertEquals(1, egg.currentSpriteFrame(nowMs = hatchStartMs))
        assertEquals(2, egg.currentSpriteFrame(nowMs = hatchStartMs + EggConfig.HATCH_FRAME_DELAY_MS))
        assertEquals(3, egg.currentSpriteFrame(nowMs = hatchStartMs + EggConfig.HATCH_FRAME_DELAY_MS * 2))
        assertFalse(egg.isReadyToHatch)

        egg.tick(nowMs = hatchStartMs + crackDurationMs)
        assertTrue(egg.isReadyToHatch)
        assertEquals(4, egg.currentSpriteFrame(nowMs = hatchStartMs + crackDurationMs))
    }

    @Test
    fun `hatch produces a chick at the egg position and keeps the shell visible`() {
        val (egg, hatchStartMs) = eggAtHatchStart()
        val readyMs = hatchStartMs + crackDurationMs
        egg.tick(nowMs = readyMs)

        val chick = egg.hatch(newChickId = 99, nowMs = readyMs)

        assertEquals(99L, chick.id)
        assertTrue(chick.x == 12.0 && chick.y == 34.0)
        assertTrue(egg.hatchConfirmed)
        assertFalse(egg.isReadyToRemove)
        assertEquals(4, egg.currentSpriteFrame(nowMs = readyMs))
    }

    @Test
    fun `becomes ready to remove only after the shell has been visible long enough`() {
        val (egg, hatchStartMs) = eggAtHatchStart()
        val readyMs = hatchStartMs + crackDurationMs
        egg.tick(nowMs = readyMs)
        egg.hatch(newChickId = 99, nowMs = readyMs)

        egg.tick(nowMs = readyMs + EggConfig.SHELL_VISIBLE_MS - 1)
        assertFalse(egg.isReadyToRemove)

        egg.tick(nowMs = readyMs + EggConfig.SHELL_VISIBLE_MS)
        assertTrue(egg.isReadyToRemove)
        assertEquals(EggState.HATCHED, egg.state)
    }
}
