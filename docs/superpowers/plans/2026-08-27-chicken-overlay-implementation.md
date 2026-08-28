# Chicken Overlay App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Native Android app that draws a small chicken farm sim (wander, feed, lay/sit/hatch eggs, chicks growing into chickens, chickens dying at a random age) as a system overlay on top of the home screen and other apps.

**Architecture:** One foreground `OverlayService` runs a ~30fps tick loop over a pure-Kotlin `Flock` simulation (`com.juleah.chickens.sim`, no Android dependencies, unit-tested). Each chicken/chick/egg/feed-pile/feed-bag is rendered as its own small `WindowManager`-added `ImageView` (`com.juleah.chickens.render`), positioned by absolute x/y, so the rest of the screen stays touchable. `MainActivity` is just a permission-prompt + start/stop toggle.

**Tech Stack:** Kotlin, Android SDK (minSdk 19 / targetSdk 34), JUnit 4 for local unit tests, no other libraries.

**Spec:** `docs/superpowers/specs/2026-08-27-chicken-overlay-design.md`

## Global Constraints

- `minSdk 19` (KitKat), `targetSdk` latest stable — overlay permission and window type both branch on `Build.VERSION.SDK_INT` (see spec's "Target platform" section for exact version cutoffs: 23 for runtime overlay permission, 26 for `TYPE_APPLICATION_OVERLAY` + foreground service notification).
- No overlay view may cover the full screen — every entity is its own small window so touches pass through everywhere except directly on a chicken/chick/egg/feed element.
- Tick rate: 30fps (`33ms` per tick), not 60fps — battery cost matters more than smoothness for a background overlay.
- Chicken lifespan: random `5-15 minutes` from birth. Chick growth: random `1-3 minutes` from hatch. Egg sitting requirement: random `45-90 seconds` from when a chicken starts sitting.
- Population stays between `5` and `15` (chickens + chicks): below 5 spawns a fresh adult directly; at/above 15 suppresses egg-laying.
- Assets already staged at `assets/chick.PNG`, `assets/chicken.PNG`, `assets/egg.PNG`, `assets/feed-bag.PNG`, `assets/feed.PNG` in the project root — move into `res/drawable` with lowercase, underscore-only names (Android resource naming requirement).
- Package/applicationId: `com.juleah.chickens`.

---

## Task 1: Project scaffolding

**Files:**
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `gradle.properties`
- Create: `app/build.gradle`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/layout/activity_main.xml`
- Create: `app/src/main/java/com/juleah/chickens/MainActivity.kt`

**Interfaces:**
- Produces: a buildable Android app shell with `MainActivity` showing a status label and a toggle button (inert for now — wired up in Task 10).

- [ ] **Step 1: Create `settings.gradle`**

```groovy
rootProject.name = "Chickens"
include ':app'
```

- [ ] **Step 2: Create root `build.gradle`**

```groovy
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.5.0'
        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24'
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
```

- [ ] **Step 3: Create `gradle.properties`**

```
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
kotlin.code.style=official
```

- [ ] **Step 4: Create `app/build.gradle`**

```groovy
apply plugin: 'com.android.application'
apply plugin: 'kotlin-android'

android {
    namespace 'com.juleah.chickens'
    compileSdk 34

    defaultConfig {
        applicationId "com.juleah.chickens"
        minSdk 19
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.9.24'
    testImplementation 'junit:junit:4.13.2'
}
```

- [ ] **Step 5: Create `app/src/main/res/values/strings.xml`**

```xml
<resources>
    <string name="app_name">Chickens</string>
    <string name="status_stopped">Chickens are not running</string>
    <string name="status_running">Chickens are running</string>
    <string name="start_chickens">Start Chickens</string>
    <string name="stop_chickens">Stop Chickens</string>
</resources>
```

- [ ] **Step 6: Create `app/src/main/res/layout/activity_main.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center"
    android:padding="24dp">

    <TextView
        android:id="@+id/statusText"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textSize="16sp"
        android:text="@string/status_stopped" />

    <Button
        android:id="@+id/toggleButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:text="@string/start_chickens" />

</LinearLayout>
```

- [ ] **Step 7: Create `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:theme="@android:style/Theme.DeviceDefault.Light">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>
</manifest>
```

- [ ] **Step 8: Create `app/src/main/java/com/juleah/chickens/MainActivity.kt`**

```kotlin
package com.juleah.chickens

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.statusText)
        val toggleButton = findViewById<Button>(R.id.toggleButton)

        statusText.text = getString(R.string.status_stopped)
        toggleButton.text = getString(R.string.start_chickens)
    }
}
```

- [ ] **Step 9: Generate the Gradle wrapper**

Run: `gradle wrapper --gradle-version 8.7` from the project root.

If `gradle` isn't on PATH, open the project once in Android Studio (it generates `gradlew`/`gradlew.bat`/`gradle/wrapper/*` automatically), then continue.

- [ ] **Step 10: Build and verify**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`, producing `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 11: Commit**

```bash
git add settings.gradle build.gradle gradle.properties app/build.gradle app/src/main/AndroidManifest.xml app/src/main/res app/src/main/java gradlew gradlew.bat gradle
git commit -m "chore: scaffold Android project shell"
```

---

## Task 2: Core sim primitives — Config, Clock, Rng

**Files:**
- Create: `app/src/main/java/com/juleah/chickens/sim/Config.kt`
- Create: `app/src/main/java/com/juleah/chickens/sim/Clock.kt`
- Create: `app/src/main/java/com/juleah/chickens/sim/Rng.kt`
- Test: `app/src/test/java/com/juleah/chickens/sim/RandomRngTest.kt`

**Interfaces:**
- Produces: `ChickenConfig`, `ChickConfig`, `EggConfig`, `FeedConfig`, `PopulationConfig` (constant objects); `Clock` interface + `SystemClock`/`FakeClock`; `Rng` interface + `RandomRng`/`FakeRng` with `nextDouble(): Double` and `nextLongInRange(min: Long, max: Long): Long` (returns a value in `[min, max)`, or `min` when `min == max`).

- [ ] **Step 1: Write the failing test**

```kotlin
package com.juleah.chickens.sim

import org.junit.Assert.assertTrue
import org.junit.Test

class RandomRngTest {
    @Test
    fun `nextLongInRange stays within bounds across many samples`() {
        val rng = RandomRng()
        repeat(1000) {
            val value = rng.nextLongInRange(10L, 20L)
            assertTrue(value in 10L..19L)
        }
    }

    @Test
    fun `nextLongInRange returns min when min equals max`() {
        val rng = RandomRng()
        assertTrue(rng.nextLongInRange(5L, 5L) == 5L)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.juleah.chickens.sim.RandomRngTest"`
Expected: FAIL to compile — `RandomRng` is unresolved.

- [ ] **Step 3: Create `Config.kt`**

```kotlin
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
```

- [ ] **Step 4: Create `Clock.kt`**

```kotlin
package com.juleah.chickens.sim

interface Clock {
    fun nowMs(): Long
}

class SystemClock : Clock {
    override fun nowMs(): Long = System.currentTimeMillis()
}

class FakeClock(private var time: Long = 0L) : Clock {
    override fun nowMs(): Long = time
    fun advanceBy(ms: Long) { time += ms }
    fun set(ms: Long) { time = ms }
}
```

- [ ] **Step 5: Create `Rng.kt`**

```kotlin
package com.juleah.chickens.sim

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
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.juleah.chickens.sim.RandomRngTest"`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/juleah/chickens/sim/Config.kt app/src/main/java/com/juleah/chickens/sim/Clock.kt app/src/main/java/com/juleah/chickens/sim/Rng.kt app/src/test/java/com/juleah/chickens/sim/RandomRngTest.kt
git commit -m "feat: add sim config, clock, and rng primitives"
```

---

## Task 3: ChickenEntity death lifecycle

**Files:**
- Create: `app/src/main/java/com/juleah/chickens/sim/ChickenEntity.kt`
- Test: `app/src/test/java/com/juleah/chickens/sim/ChickenEntityTest.kt`

**Interfaces:**
- Consumes: `Rng` (Task 2).
- Produces: `ChickenEntity(id: Long, x: Double, y: Double, birthTimeMs: Long, rng: Rng)` with `val deathAtMs: Long`, `val isDead: Boolean`, `fun tick(nowMs: Long)`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.juleah.chickens.sim

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChickenEntityTest {

    @Test
    fun `death time is within configured lifespan bounds`() {
        val rng = RandomRng()
        val birth = 1_000_000L
        val chicken = ChickenEntity(id = 1, x = 0.0, y = 0.0, birthTimeMs = birth, rng = rng)

        val lifespan = chicken.deathAtMs - birth
        assertTrue(lifespan >= ChickenConfig.MIN_LIFESPAN_MS)
        assertTrue(lifespan <= ChickenConfig.MAX_LIFESPAN_MS)
    }

    @Test
    fun `chicken is not dead before its death time`() {
        val rng = FakeRng(longValue = ChickenConfig.MIN_LIFESPAN_MS)
        val chicken = ChickenEntity(id = 1, x = 0.0, y = 0.0, birthTimeMs = 0L, rng = rng)

        chicken.tick(nowMs = chicken.deathAtMs - 1)

        assertFalse(chicken.isDead)
    }

    @Test
    fun `chicken dies once its death time is reached`() {
        val rng = FakeRng(longValue = ChickenConfig.MIN_LIFESPAN_MS)
        val chicken = ChickenEntity(id = 1, x = 0.0, y = 0.0, birthTimeMs = 0L, rng = rng)

        chicken.tick(nowMs = chicken.deathAtMs)

        assertTrue(chicken.isDead)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.juleah.chickens.sim.ChickenEntityTest"`
Expected: FAIL to compile — `ChickenEntity` is unresolved.

- [ ] **Step 3: Create `ChickenEntity.kt`**

```kotlin
package com.juleah.chickens.sim

enum class ChickenAnimState { IDLE, WALKING, PECKING, JUMPING, SITTING }

class ChickenEntity(
    val id: Long,
    var x: Double,
    var y: Double,
    birthTimeMs: Long,
    private val rng: Rng
) {
    val deathAtMs: Long = birthTimeMs + rng.nextLongInRange(ChickenConfig.MIN_LIFESPAN_MS, ChickenConfig.MAX_LIFESPAN_MS)
    var isDead: Boolean = false
        private set

    var animState: ChickenAnimState = ChickenAnimState.IDLE
        private set

    fun tick(nowMs: Long) {
        if (nowMs >= deathAtMs) {
            isDead = true
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.juleah.chickens.sim.ChickenEntityTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/juleah/chickens/sim/ChickenEntity.kt app/src/test/java/com/juleah/chickens/sim/ChickenEntityTest.kt
git commit -m "feat: add chicken death lifecycle"
```

---

## Task 4: ChickEntity growth lifecycle

**Files:**
- Create: `app/src/main/java/com/juleah/chickens/sim/ChickEntity.kt`
- Test: `app/src/test/java/com/juleah/chickens/sim/ChickEntityTest.kt`

**Interfaces:**
- Consumes: `Rng` (Task 2), `ChickenEntity` (Task 3).
- Produces: `ChickEntity(id: Long, x: Double, y: Double, parentId: Long?, hatchTimeMs: Long, rng: Rng)` with `val growUpAtMs: Long`, `val hasGrownUp: Boolean`, `fun tick(nowMs: Long)`, `fun toChicken(nowMs: Long, rng: Rng): ChickenEntity`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.juleah.chickens.sim

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChickEntityTest {

    @Test
    fun `growth time is within configured bounds`() {
        val rng = RandomRng()
        val chick = ChickEntity(id = 1, x = 0.0, y = 0.0, parentId = null, hatchTimeMs = 1_000_000L, rng = rng)

        val growthDuration = chick.growUpAtMs - 1_000_000L
        assertTrue(growthDuration >= ChickConfig.MIN_GROWTH_MS)
        assertTrue(growthDuration <= ChickConfig.MAX_GROWTH_MS)
    }

    @Test
    fun `chick has not grown up before its growth time`() {
        val rng = FakeRng(longValue = ChickConfig.MIN_GROWTH_MS)
        val chick = ChickEntity(id = 1, x = 0.0, y = 0.0, parentId = null, hatchTimeMs = 0L, rng = rng)

        chick.tick(nowMs = chick.growUpAtMs - 1)

        assertFalse(chick.hasGrownUp)
    }

    @Test
    fun `chick grows up once its growth time is reached`() {
        val rng = FakeRng(longValue = ChickConfig.MIN_GROWTH_MS)
        val chick = ChickEntity(id = 1, x = 0.0, y = 0.0, parentId = null, hatchTimeMs = 0L, rng = rng)

        chick.tick(nowMs = chick.growUpAtMs)

        assertTrue(chick.hasGrownUp)
    }

    @Test
    fun `toChicken carries over id and position with a fresh lifespan`() {
        val rng = FakeRng(longValue = ChickConfig.MIN_GROWTH_MS)
        val chick = ChickEntity(id = 42, x = 7.0, y = 8.0, parentId = null, hatchTimeMs = 0L, rng = rng)

        val chicken = chick.toChicken(nowMs = chick.growUpAtMs, rng = rng)

        assertTrue(chicken.id == 42L)
        assertTrue(chicken.x == 7.0)
        assertTrue(chicken.y == 8.0)
        assertTrue(chicken.deathAtMs > chick.growUpAtMs)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.juleah.chickens.sim.ChickEntityTest"`
Expected: FAIL to compile — `ChickEntity` is unresolved.

- [ ] **Step 3: Create `ChickEntity.kt`**

```kotlin
package com.juleah.chickens.sim

enum class ChickAnimState { IDLE, WALKING, RUNNING }

class ChickEntity(
    val id: Long,
    var x: Double,
    var y: Double,
    val parentId: Long?,
    hatchTimeMs: Long,
    private val rng: Rng
) {
    val growUpAtMs: Long = hatchTimeMs + rng.nextLongInRange(ChickConfig.MIN_GROWTH_MS, ChickConfig.MAX_GROWTH_MS)
    var hasGrownUp: Boolean = false
        private set

    var animState: ChickAnimState = ChickAnimState.IDLE
        private set

    fun tick(nowMs: Long) {
        if (nowMs >= growUpAtMs) {
            hasGrownUp = true
        }
    }

    fun toChicken(nowMs: Long, rng: Rng): ChickenEntity =
        ChickenEntity(id = id, x = x, y = y, birthTimeMs = nowMs, rng = rng)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.juleah.chickens.sim.ChickEntityTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/juleah/chickens/sim/ChickEntity.kt app/src/test/java/com/juleah/chickens/sim/ChickEntityTest.kt
git commit -m "feat: add chick growth lifecycle"
```

---

## Task 5: EggEntity sitting lifecycle

**Files:**
- Create: `app/src/main/java/com/juleah/chickens/sim/EggEntity.kt`
- Test: `app/src/test/java/com/juleah/chickens/sim/EggEntityTest.kt`

**Interfaces:**
- Consumes: `Rng` (Task 2), `ChickEntity` (Task 4).
- Produces: `EggState` enum (`WAITING, SITTING, HATCHING, HATCHED`); `EggEntity(id: Long, x: Double, y: Double, laidByChickenId: Long, rng: Rng)` with `val state: EggState`, `fun startSitting(chickenId: Long, nowMs: Long)`, `fun sittingChickenIdOrNull(): Long?`, `fun tick(nowMs: Long)`, `fun hatch(newChickId: Long, nowMs: Long): ChickEntity`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.juleah.chickens.sim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EggEntityTest {

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
    fun `moves to hatching once its sitting time elapses`() {
        val rng = FakeRng(longValue = EggConfig.MIN_SITTING_REQUIRED_MS)
        val egg = EggEntity(id = 1, x = 0.0, y = 0.0, laidByChickenId = 5, rng = rng)
        egg.startSitting(chickenId = 5, nowMs = 0L)

        egg.tick(nowMs = EggConfig.MIN_SITTING_REQUIRED_MS)

        assertEquals(EggState.HATCHING, egg.state)
    }

    @Test
    fun `hatch produces a chick at the egg position`() {
        val rng = FakeRng(longValue = EggConfig.MIN_SITTING_REQUIRED_MS)
        val egg = EggEntity(id = 1, x = 12.0, y = 34.0, laidByChickenId = 5, rng = rng)
        egg.startSitting(chickenId = 5, nowMs = 0L)
        egg.tick(nowMs = EggConfig.MIN_SITTING_REQUIRED_MS)

        val chick = egg.hatch(newChickId = 99, nowMs = EggConfig.MIN_SITTING_REQUIRED_MS)

        assertEquals(EggState.HATCHED, egg.state)
        assertEquals(99L, chick.id)
        assertTrue(chick.x == 12.0 && chick.y == 34.0)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.juleah.chickens.sim.EggEntityTest"`
Expected: FAIL to compile — `EggEntity` is unresolved.

- [ ] **Step 3: Create `EggEntity.kt`**

```kotlin
package com.juleah.chickens.sim

enum class EggState { WAITING, SITTING, HATCHING, HATCHED }

class EggEntity(
    val id: Long,
    val x: Double,
    val y: Double,
    val laidByChickenId: Long,
    private val rng: Rng
) {
    var state: EggState = EggState.WAITING
        private set

    private var sittingChickenId: Long? = null
    private var sittingStartMs: Long = 0L
    private val sittingRequiredMs: Long =
        rng.nextLongInRange(EggConfig.MIN_SITTING_REQUIRED_MS, EggConfig.MAX_SITTING_REQUIRED_MS)

    fun startSitting(chickenId: Long, nowMs: Long) {
        if (state != EggState.WAITING || sittingChickenId != null) return
        sittingChickenId = chickenId
        sittingStartMs = nowMs
        state = EggState.SITTING
    }

    fun sittingChickenIdOrNull(): Long? = sittingChickenId

    fun tick(nowMs: Long) {
        if (state == EggState.SITTING && sittingChickenId != null) {
            if (nowMs - sittingStartMs >= sittingRequiredMs) {
                state = EggState.HATCHING
            }
        }
    }

    fun hatch(newChickId: Long, nowMs: Long): ChickEntity {
        state = EggState.HATCHED
        return ChickEntity(id = newChickId, x = x, y = y, parentId = laidByChickenId, hatchTimeMs = nowMs, rng = rng)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.juleah.chickens.sim.EggEntityTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/juleah/chickens/sim/EggEntity.kt app/src/test/java/com/juleah/chickens/sim/EggEntityTest.kt
git commit -m "feat: add egg sitting/hatching lifecycle"
```

---

## Task 6: PopulationRegulator

**Files:**
- Create: `app/src/main/java/com/juleah/chickens/sim/PopulationRegulator.kt`
- Test: `app/src/test/java/com/juleah/chickens/sim/PopulationRegulatorTest.kt`

**Interfaces:**
- Consumes: `PopulationConfig` (Task 2).
- Produces: `PopulationAction` enum (`SPAWN_ADULT, SUPPRESS_EGG_LAYING, NORMAL`); `PopulationRegulator.decide(currentCount: Int): PopulationAction`.

- [ ] **Step 1: Write the failing test**

```kotlin
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.juleah.chickens.sim.PopulationRegulatorTest"`
Expected: FAIL to compile — `PopulationRegulator` is unresolved.

- [ ] **Step 3: Create `PopulationRegulator.kt`**

```kotlin
package com.juleah.chickens.sim

enum class PopulationAction { SPAWN_ADULT, SUPPRESS_EGG_LAYING, NORMAL }

object PopulationRegulator {
    fun decide(currentCount: Int): PopulationAction = when {
        currentCount < PopulationConfig.MIN_POPULATION -> PopulationAction.SPAWN_ADULT
        currentCount >= PopulationConfig.MAX_POPULATION -> PopulationAction.SUPPRESS_EGG_LAYING
        else -> PopulationAction.NORMAL
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.juleah.chickens.sim.PopulationRegulatorTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/juleah/chickens/sim/PopulationRegulator.kt app/src/test/java/com/juleah/chickens/sim/PopulationRegulatorTest.kt
git commit -m "feat: add population regulation decision table"
```

---

## Task 7: Flock integration

**Files:**
- Create: `app/src/main/java/com/juleah/chickens/sim/Flock.kt`
- Test: `app/src/test/java/com/juleah/chickens/sim/FlockTest.kt`

**Interfaces:**
- Consumes: `Clock`, `Rng` (Task 2); `ChickenEntity` (Task 3); `ChickEntity` (Task 4); `EggEntity`, `EggState` (Task 5); `PopulationRegulator`, `PopulationAction` (Task 6).
- Produces: `Flock(clock: Clock, rng: Rng, screenWidthPx: Double, screenHeightPx: Double)` with mutable lists `chickens: MutableList<ChickenEntity>`, `chicks: MutableList<ChickEntity>`, `eggs: MutableList<EggEntity>`; `fun populationCount(): Int`, `fun canLayEgg(): Boolean`, `fun seedInitialPopulation()`, `fun spawnAdultChicken(): ChickenEntity`, `fun layEgg(chickenId: Long, x: Double, y: Double): EggEntity`, `fun eggsReadyToHatch(): List<EggEntity>`, `fun confirmHatch(eggId: Long)`, `fun tick()`. Later tasks (14, 15) extend this file with egg-roll helpers and feed piles.

- [ ] **Step 1: Write the failing test**

```kotlin
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.juleah.chickens.sim.FlockTest"`
Expected: FAIL to compile — `Flock` is unresolved.

- [ ] **Step 3: Create `Flock.kt`**

```kotlin
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.juleah.chickens.sim.FlockTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/juleah/chickens/sim/Flock.kt app/src/test/java/com/juleah/chickens/sim/FlockTest.kt
git commit -m "feat: wire flock lifecycle and population regulation together"
```

---

## Task 8: Overlay permission helper

No automated test — this task is a thin wrapper around `Settings.canDrawOverlays`, `ACTION_MANAGE_OVERLAY_PERMISSION`, and the SDK-dependent overlay window type, all of which require a real Android runtime to exercise meaningfully. Verified manually in Task 10.

**Files:**
- Create: `app/src/main/java/com/juleah/chickens/overlay/OverlayPermission.kt`

**Interfaces:**
- Produces: `OverlayPermission.isGranted(context: Context): Boolean`, `OverlayPermission.requestPermission(activity: Activity, requestCode: Int)`, `OverlayPermission.overlayWindowType(): Int`.

- [ ] **Step 1: Create `OverlayPermission.kt`**

```kotlin
package com.juleah.chickens.overlay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.WindowManager

object OverlayPermission {

    fun isGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun requestPermission(activity: Activity, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + activity.packageName)
            )
            activity.startActivityForResult(intent, requestCode)
        }
    }

    fun overlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/juleah/chickens/overlay/OverlayPermission.kt
git commit -m "feat: add SDK-branched overlay permission helper"
```

---

## Task 9: OverlayService skeleton with tick loop

**Files:**
- Create: `app/src/main/java/com/juleah/chickens/OverlayService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `Flock`, `SystemClock`, `RandomRng` (Task 7/2); `OverlayPermission` (Task 8, used starting Task 11).
- Produces: `OverlayService`, a `Service` that seeds a `Flock` sized to the display and ticks it at 30fps. Exposes package-private fields `windowManager`, `screenWidthPx`, `screenHeightPx`, `flock` for later tasks to extend.

- [ ] **Step 1: Modify `AndroidManifest.xml` — declare the service**

In `<application>`, after the `<activity>` block, add:

```xml
        <service
            android:name=".OverlayService"
            android:enabled="true"
            android:exported="false" />
```

- [ ] **Step 2: Create `OverlayService.kt`**

```kotlin
package com.juleah.chickens

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.juleah.chickens.sim.Flock
import com.juleah.chickens.sim.RandomRng
import com.juleah.chickens.sim.SystemClock

class OverlayService : Service() {

    companion object {
        private const val TAG = "OverlayService"
        private const val NOTIFICATION_CHANNEL_ID = "chickens_overlay"
        private const val NOTIFICATION_ID = 1
        const val TICK_INTERVAL_MS = 33L // ~30fps
    }

    lateinit var windowManager: WindowManager
        private set
    var screenWidthPx: Double = 0.0
        private set
    var screenHeightPx: Double = 0.0
        private set
    lateinit var flock: Flock
        private set

    private val tickHandler = Handler(Looper.getMainLooper())
    private var isRunning = false

    private val tickRunnable = object : Runnable {
        override fun run() {
            flock.tick()
            Log.d(TAG, "population=${flock.populationCount()}")
            if (isRunning) {
                tickHandler.postDelayed(this, TICK_INTERVAL_MS)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(metrics)
        screenWidthPx = metrics.widthPixels.toDouble()
        screenHeightPx = metrics.heightPixels.toDouble()

        flock = Flock(
            clock = SystemClock(),
            rng = RandomRng(),
            screenWidthPx = screenWidthPx,
            screenHeightPx = screenHeightPx
        )
        flock.seedInitialPopulation()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        if (!isRunning) {
            isRunning = true
            tickHandler.post(tickRunnable)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        tickHandler.removeCallbacks(tickRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_MIN
        )
        manager.createNotificationChannel(channel)
        return Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.status_running))
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .build()
    }
}
```

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Manual verify — service lifecycle**

Install: `./gradlew installDebug`
Start it directly: `adb shell am start-service -n com.juleah.chickens/.OverlayService`
Watch logs: `adb logcat -s OverlayService`
Expected: a `population=8` (or similar) line roughly every tick.
On an API 26+ device/emulator, check the notification shade for a persistent low-priority "Chickens" notification.
Stop it: `adb shell am force-stop com.juleah.chickens`
Expected: log lines stop.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/java/com/juleah/chickens/OverlayService.kt
git commit -m "feat: add overlay service skeleton with 30fps flock tick loop"
```

---

## Task 10: MainActivity start/stop wiring

**Files:**
- Modify: `app/src/main/java/com/juleah/chickens/MainActivity.kt`

**Interfaces:**
- Consumes: `OverlayPermission` (Task 8), `OverlayService` (Task 9).

- [ ] **Step 1: Replace `MainActivity.kt` contents**

```kotlin
package com.juleah.chickens

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.juleah.chickens.overlay.OverlayPermission

class MainActivity : Activity() {

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1001
    }

    private var isServiceRunning = false
    private lateinit var statusText: TextView
    private lateinit var toggleButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        toggleButton = findViewById(R.id.toggleButton)

        toggleButton.setOnClickListener {
            if (isServiceRunning) stopOverlay() else startOverlayOrRequestPermission()
        }

        updateUi()
    }

    private fun startOverlayOrRequestPermission() {
        if (OverlayPermission.isGranted(this)) {
            startOverlay()
        } else {
            OverlayPermission.requestPermission(this, OVERLAY_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE && OverlayPermission.isGranted(this)) {
            startOverlay()
        }
    }

    private fun startOverlay() {
        startService(Intent(this, OverlayService::class.java))
        isServiceRunning = true
        updateUi()
    }

    private fun stopOverlay() {
        stopService(Intent(this, OverlayService::class.java))
        isServiceRunning = false
        updateUi()
    }

    private fun updateUi() {
        statusText.text = getString(if (isServiceRunning) R.string.status_running else R.string.status_stopped)
        toggleButton.text = getString(if (isServiceRunning) R.string.stop_chickens else R.string.start_chickens)
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Manual verify — full permission + toggle flow**

Install and launch the app: `./gradlew installDebug`, open "Chickens" from the launcher.
Tap "Start Chickens". On API 23+, this opens the "draw over other apps" settings screen — enable it and return (back button); the service should start automatically per `onActivityResult`. On API < 23, it starts immediately.
Expected: button now reads "Stop Chickens", `adb logcat -s OverlayService` shows population ticks.
Tap "Stop Chickens".
Expected: button reverts, log ticks stop.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/juleah/chickens/MainActivity.kt
git commit -m "feat: wire start/stop toggle to overlay permission and service"
```

---

## Task 11: Sprite slicing and chicken rendering

Copies the staged PNGs into `res/drawable` (lowercase, underscore names required by Android resource naming) and renders `Flock.chickens` as real overlay windows for the first time. No wander/animation yet — chickens appear at their spawn position and only move when the population regulator replaces one; movement is added in Task 12.

**Files:**
- Create: `app/src/main/res/drawable/chicken.png` (copied from `assets/chicken.PNG`)
- Create: `app/src/main/res/drawable/chick.png` (copied from `assets/chick.PNG`)
- Create: `app/src/main/res/drawable/egg.png` (copied from `assets/egg.PNG`)
- Create: `app/src/main/res/drawable/feed.png` (copied from `assets/feed.PNG`)
- Create: `app/src/main/res/drawable/feed_bag.png` (copied from `assets/feed-bag.PNG`)
- Create: `app/src/main/java/com/juleah/chickens/render/SpriteSheet.kt`
- Create: `app/src/main/java/com/juleah/chickens/render/EntityView.kt`
- Create: `app/src/main/java/com/juleah/chickens/render/OverlayRenderer.kt`
- Modify: `app/src/main/java/com/juleah/chickens/OverlayService.kt`

**Interfaces:**
- Consumes: `OverlayPermission.overlayWindowType()` (Task 8), `Flock` (Task 7), `ChickenConfig` (Task 2).
- Produces: `SpriteSheet(context, drawableResId, frameSizePx)` with `fun frame(index: Int): Bitmap`; `EntityView(context, windowManager, sizePx)` with `val imageView: ImageView`, `fun show(x: Double, y: Double)`, `fun remove()`; `OverlayRenderer(context, windowManager)` with `fun render(flock: Flock)`, `fun clear()`.

- [ ] **Step 1: Copy and rename the sprite assets**

```bash
mkdir -p app/src/main/res/drawable
cp assets/chicken.PNG app/src/main/res/drawable/chicken.png
cp assets/chick.PNG app/src/main/res/drawable/chick.png
cp assets/egg.PNG app/src/main/res/drawable/egg.png
cp assets/feed.PNG app/src/main/res/drawable/feed.png
cp assets/feed-bag.PNG app/src/main/res/drawable/feed_bag.png
```

- [ ] **Step 2: Create `SpriteSheet.kt`**

```kotlin
package com.juleah.chickens.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

class SpriteSheet(context: Context, drawableResId: Int, private val frameSizePx: Int) {
    private val sheet: Bitmap = BitmapFactory.decodeResource(context.resources, drawableResId)
    private val frameCount: Int = (sheet.width / frameSizePx).coerceAtLeast(1)
    private val frames: Array<Bitmap> = Array(frameCount) { index ->
        Bitmap.createBitmap(sheet, index * frameSizePx, 0, frameSizePx, frameSizePx)
    }

    fun frame(index: Int): Bitmap = frames[index % frames.size]
}
```

- [ ] **Step 3: Create `EntityView.kt`**

```kotlin
package com.juleah.chickens.render

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageView
import com.juleah.chickens.overlay.OverlayPermission

class EntityView(
    context: Context,
    private val windowManager: WindowManager,
    sizePx: Int
) {
    val imageView = ImageView(context)
    private val layoutParams = WindowManager.LayoutParams(
        sizePx,
        sizePx,
        OverlayPermission.overlayWindowType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }
    private var added = false

    fun show(x: Double, y: Double) {
        layoutParams.x = x.toInt()
        layoutParams.y = y.toInt()
        if (!added) {
            windowManager.addView(imageView, layoutParams)
            added = true
        } else {
            windowManager.updateViewLayout(imageView, layoutParams)
        }
    }

    fun remove() {
        if (added) {
            windowManager.removeView(imageView)
            added = false
        }
    }
}
```

- [ ] **Step 4: Create `OverlayRenderer.kt`**

```kotlin
package com.juleah.chickens.render

import android.content.Context
import android.view.WindowManager
import com.juleah.chickens.R
import com.juleah.chickens.sim.ChickenConfig
import com.juleah.chickens.sim.Flock

class OverlayRenderer(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private val chickenSprite = SpriteSheet(context, R.drawable.chicken, ChickenConfig.SIZE_PX)
    private val chickenViews = mutableMapOf<Long, EntityView>()

    fun render(flock: Flock) {
        val liveIds = flock.chickens.map { it.id }.toSet()
        chickenViews.keys.filterNot { it in liveIds }.forEach { staleId ->
            chickenViews.remove(staleId)?.remove()
        }

        flock.chickens.forEach { chicken ->
            val view = chickenViews.getOrPut(chicken.id) {
                EntityView(context, windowManager, ChickenConfig.SIZE_PX)
            }
            view.imageView.setImageBitmap(chickenSprite.frame(0))
            view.show(chicken.x, chicken.y)
        }
    }

    fun clear() {
        chickenViews.values.forEach { it.remove() }
        chickenViews.clear()
    }
}
```

- [ ] **Step 5: Modify `OverlayService.kt` — wire the renderer in**

Add the import `import com.juleah.chickens.render.OverlayRenderer`, add a field:

```kotlin
    private lateinit var renderer: OverlayRenderer
```

At the end of `onCreate()`, after `flock.seedInitialPopulation()`:

```kotlin
        renderer = OverlayRenderer(this, windowManager)
```

In `tickRunnable.run()`, after the `Log.d(...)` line and before the `if (isRunning)` check:

```kotlin
            renderer.render(flock)
```

In `onDestroy()`, before `super.onDestroy()`:

```kotlin
        renderer.clear()
```

- [ ] **Step 6: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Manual verify — chickens appear on screen**

Install and start the overlay (Task 10's toggle button).
Expected: 8 static chicken sprites appear scattered on screen, on top of the home screen/other apps, and stay touchable everywhere else.
Since chickens don't die for 5-15 minutes by default, to see a death+respawn quickly for this verification: temporarily set `ChickenConfig.MIN_LIFESPAN_MS`/`MAX_LIFESPAN_MS` to a few seconds, rebuild, observe a chicken vanish and a new one appear, then revert the values before committing.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/res/drawable app/src/main/java/com/juleah/chickens/render app/src/main/java/com/juleah/chickens/OverlayService.kt
git commit -m "feat: render chickens as overlay windows"
```

---

## Task 12: Chicken wander, idle, peck, jump, and animation

**Files:**
- Modify: `app/src/main/java/com/juleah/chickens/sim/ChickenEntity.kt`
- Modify: `app/src/main/java/com/juleah/chickens/render/OverlayRenderer.kt`
- Modify: `app/src/main/java/com/juleah/chickens/OverlayService.kt`

Ported directly from mombotro's `chicken.js` `wander()`, with one simplification noted where it applies: jump freezes the chicken in place for `JUMP_DURATION_MS` rather than animating an arc — a real, deliberate simplification, not a placeholder.

**Interfaces:**
- Produces (additions to `ChickenEntity`): `fun wander(deltaMs: Long, screenWidthPx: Double, screenHeightPx: Double)`, `fun jump()`, `val facingRight: Boolean`.

- [ ] **Step 1: Replace `ChickenEntity.kt` contents**

```kotlin
package com.juleah.chickens.sim

enum class ChickenAnimState { IDLE, WALKING, PECKING, JUMPING, SITTING }

class ChickenEntity(
    val id: Long,
    var x: Double,
    var y: Double,
    birthTimeMs: Long,
    private val rng: Rng
) {
    val deathAtMs: Long = birthTimeMs + rng.nextLongInRange(ChickenConfig.MIN_LIFESPAN_MS, ChickenConfig.MAX_LIFESPAN_MS)
    var isDead: Boolean = false
        private set

    var animState: ChickenAnimState = ChickenAnimState.IDLE
        private set
    var facingRight: Boolean = true
        private set

    private var velocityX: Double = (rng.nextDouble() - 0.5) * ChickenConfig.MAX_VELOCITY
    private var velocityY: Double = (rng.nextDouble() - 0.5) * ChickenConfig.MAX_VELOCITY

    private var isIdling = false
    private var idleRemainingMs = 0L
    private var isPecking = false
    private var peckRemainingMs = 0L
    private var isJumping = false
    private var jumpRemainingMs = 0L
    private var feedPeckRemainingMs = 0L
    private var targetFeedId: Long? = null

    fun tick(nowMs: Long) {
        if (nowMs >= deathAtMs) {
            isDead = true
        }
    }

    fun currentTargetFeedId(): Long? = targetFeedId
    fun clearTargetFeed() { targetFeedId = null }
    fun rushToFeed(feedId: Long) {
        targetFeedId = feedId
        isIdling = false
        isPecking = false
    }

    /** Ported from mombotro's chicken.js wander(). */
    fun wander(deltaMs: Long, screenWidthPx: Double, screenHeightPx: Double) {
        if (isJumping) {
            jumpRemainingMs -= deltaMs
            if (jumpRemainingMs <= 0) {
                isJumping = false
                animState = movingAnimState()
            }
            return
        }

        if (isPecking) {
            peckRemainingMs -= deltaMs
            if (peckRemainingMs <= 0) {
                isPecking = false
                animState = movingAnimState()
            }
            return
        }

        if (isIdling) {
            idleRemainingMs -= deltaMs
            if (idleRemainingMs <= 0) {
                isIdling = false
                velocityX = (rng.nextDouble() - 0.5) * ChickenConfig.MAX_VELOCITY
                velocityY = (rng.nextDouble() - 0.5) * ChickenConfig.MAX_VELOCITY
            } else {
                return
            }
        }

        if (rng.nextDouble() < ChickenConfig.DIRECTION_CHANGE_CHANCE) {
            velocityX = (rng.nextDouble() - 0.5) * ChickenConfig.MAX_VELOCITY
            velocityY = (rng.nextDouble() - 0.5) * ChickenConfig.MAX_VELOCITY
        }

        if (rng.nextDouble() < ChickenConfig.PECK_CHANCE) {
            isPecking = true
            peckRemainingMs = ChickenConfig.PECK_DURATION_MS
            animState = ChickenAnimState.PECKING
            return
        }

        if (rng.nextDouble() < ChickenConfig.IDLE_CHANCE) {
            isIdling = true
            idleRemainingMs = ChickenConfig.IDLE_MIN_DURATION_MS +
                (rng.nextDouble() * (ChickenConfig.IDLE_MAX_DURATION_MS - ChickenConfig.IDLE_MIN_DURATION_MS)).toLong()
            animState = ChickenAnimState.IDLE
            return
        }

        x += velocityX
        y += velocityY
        facingRight = velocityX > 0.1

        val size = ChickenConfig.SIZE_PX
        if (x <= 0 || x >= screenWidthPx - size) {
            velocityX = -velocityX
            x = x.coerceIn(0.0, screenWidthPx - size)
        }
        if (y <= 0 || y >= screenHeightPx - size) {
            velocityY = -velocityY
            y = y.coerceIn(0.0, screenHeightPx - size)
        }

        animState = movingAnimState()
    }

    fun jump() {
        if (isJumping || isPecking) return
        isJumping = true
        jumpRemainingMs = ChickenConfig.JUMP_DURATION_MS
        animState = ChickenAnimState.JUMPING
    }

    fun moveTowardFeed(deltaMs: Long, feedX: Double, feedY: Double): Boolean {
        if (feedPeckRemainingMs > 0) {
            feedPeckRemainingMs -= deltaMs
            animState = ChickenAnimState.PECKING
            return false
        }

        val dx = feedX - x
        val dy = feedY - y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

        if (distance <= 20.0) {
            feedPeckRemainingMs = ChickenConfig.PECK_DURATION_MS
            animState = ChickenAnimState.PECKING
            return true
        }

        val speed = 2.0
        x += (dx / distance) * speed
        y += (dy / distance) * speed
        facingRight = dx > 0
        animState = ChickenAnimState.WALKING
        return false
    }

    private fun movingAnimState(): ChickenAnimState =
        if (kotlin.math.abs(velocityX) + kotlin.math.abs(velocityY) > 0.1) {
            ChickenAnimState.WALKING
        } else {
            ChickenAnimState.IDLE
        }
}
```

- [ ] **Step 2: Modify `OverlayRenderer.kt` — animate chicken frames and wire jump-on-tap**

Replace the `render()` method's chicken loop with:

```kotlin
        flock.chickens.forEach { chicken ->
            val view = chickenViews.getOrPut(chicken.id) {
                EntityView(context, windowManager, ChickenConfig.SIZE_PX).also {
                    it.imageView.setOnClickListener { _ -> chicken.jump() }
                }
            }
            view.imageView.setImageBitmap(chickenSprite.frame(chickenFrameIndex(chicken)))
            view.show(chicken.x, chicken.y)
        }
```

Add a private helper below `render()`:

```kotlin
    private fun chickenFrameIndex(chicken: com.juleah.chickens.sim.ChickenEntity): Int =
        when (chicken.animState) {
            com.juleah.chickens.sim.ChickenAnimState.IDLE -> 0
            com.juleah.chickens.sim.ChickenAnimState.WALKING -> 2
            com.juleah.chickens.sim.ChickenAnimState.PECKING -> 6
            com.juleah.chickens.sim.ChickenAnimState.JUMPING -> 8
            com.juleah.chickens.sim.ChickenAnimState.SITTING -> 10
        }
```

(Frame offsets match mombotro's `CHICKEN_CONFIG.animations` frame ranges — first frame of each state's range.)

- [ ] **Step 3: Modify `OverlayService.kt` — call `wander()` each tick**

In `tickRunnable.run()`, before `renderer.render(flock)`, add:

```kotlin
            flock.chickens.forEach { chicken ->
                chicken.wander(TICK_INTERVAL_MS, screenWidthPx, screenHeightPx)
            }
```

- [ ] **Step 4: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Manual verify — chickens move and animate**

Start the overlay. Expected: chickens wander around, occasionally idle or peck, bounce off screen edges, and tapping one makes it briefly freeze in a "jumping" pose.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/juleah/chickens/sim/ChickenEntity.kt app/src/main/java/com/juleah/chickens/render/OverlayRenderer.kt app/src/main/java/com/juleah/chickens/OverlayService.kt
git commit -m "feat: add chicken wander/idle/peck/jump behavior and animation"
```

---

## Task 13: Chick rendering and parent-follow movement

**Files:**
- Modify: `app/src/main/java/com/juleah/chickens/sim/ChickEntity.kt`
- Modify: `app/src/main/java/com/juleah/chickens/render/OverlayRenderer.kt`
- Modify: `app/src/main/java/com/juleah/chickens/OverlayService.kt`

**Interfaces:**
- Produces (additions to `ChickEntity`): `fun followParent(deltaMs: Long, parentX: Double, parentY: Double, screenWidthPx: Double, screenHeightPx: Double)`, `fun wanderAlone(deltaMs: Long, screenWidthPx: Double, screenHeightPx: Double)`, `fun runAway()`, `val facingRight: Boolean`.

- [ ] **Step 1: Replace `ChickEntity.kt` contents**

```kotlin
package com.juleah.chickens.sim

enum class ChickAnimState { IDLE, WALKING, RUNNING }

class ChickEntity(
    val id: Long,
    var x: Double,
    var y: Double,
    val parentId: Long?,
    hatchTimeMs: Long,
    private val rng: Rng
) {
    val growUpAtMs: Long = hatchTimeMs + rng.nextLongInRange(ChickConfig.MIN_GROWTH_MS, ChickConfig.MAX_GROWTH_MS)
    var hasGrownUp: Boolean = false
        private set

    var animState: ChickAnimState = ChickAnimState.IDLE
        private set
    var facingRight: Boolean = true
        private set

    private var velocityX: Double = 0.0
    private var velocityY: Double = 0.0
    private var isRunningAway = false
    private var runAwayRemainingMs = 0L

    fun tick(nowMs: Long) {
        if (nowMs >= growUpAtMs) {
            hasGrownUp = true
        }
    }

    fun toChicken(nowMs: Long, rng: Rng): ChickenEntity =
        ChickenEntity(id = id, x = x, y = y, birthTimeMs = nowMs, rng = rng)

    fun runAway() {
        isRunningAway = true
        runAwayRemainingMs = 3000L
        velocityX = if (velocityX == 0.0) 2.0 else velocityX * 2
        velocityY = if (velocityY == 0.0) 2.0 else velocityY * 2
    }

    fun followParent(deltaMs: Long, parentX: Double, parentY: Double, screenWidthPx: Double, screenHeightPx: Double) {
        if (isRunningAway) {
            runFree(deltaMs, screenWidthPx, screenHeightPx)
            return
        }

        val dx = parentX - x
        val dy = parentY - y
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

        if (distance <= 40.0) {
            animState = ChickAnimState.IDLE
            return
        }

        val speed = 1.8
        velocityX = (dx / distance) * speed
        velocityY = (dy / distance) * speed
        move(screenWidthPx, screenHeightPx)
        animState = ChickAnimState.WALKING
    }

    fun wanderAlone(deltaMs: Long, screenWidthPx: Double, screenHeightPx: Double) {
        if (isRunningAway) {
            runFree(deltaMs, screenWidthPx, screenHeightPx)
            return
        }

        if (rng().nextDouble() < 0.01) {
            velocityX = (rng().nextDouble() - 0.5) * 1.5
            velocityY = (rng().nextDouble() - 0.5) * 1.5
        }
        move(screenWidthPx, screenHeightPx)
        animState = if (kotlin.math.abs(velocityX) + kotlin.math.abs(velocityY) > 0.1) {
            ChickAnimState.WALKING
        } else {
            ChickAnimState.IDLE
        }
    }

    private fun runFree(deltaMs: Long, screenWidthPx: Double, screenHeightPx: Double) {
        runAwayRemainingMs -= deltaMs
        if (runAwayRemainingMs <= 0) {
            isRunningAway = false
        }
        move(screenWidthPx, screenHeightPx)
        animState = ChickAnimState.RUNNING
    }

    private fun move(screenWidthPx: Double, screenHeightPx: Double) {
        x += velocityX
        y += velocityY
        facingRight = velocityX > 0.1

        val size = ChickConfig.SIZE_PX
        if (x <= 0 || x >= screenWidthPx - size) {
            velocityX = -velocityX
            x = x.coerceIn(0.0, screenWidthPx - size)
        }
        if (y <= 0 || y >= screenHeightPx - size) {
            velocityY = -velocityY
            y = y.coerceIn(0.0, screenHeightPx - size)
        }
    }

    private fun rng(): Rng = rng
}
```

- [ ] **Step 2: Modify `OverlayRenderer.kt` — add chick rendering**

Add a field alongside `chickenSprite`/`chickenViews`:

```kotlin
    private val chickSprite = SpriteSheet(context, R.drawable.chick, com.juleah.chickens.sim.ChickConfig.SIZE_PX)
    private val chickViews = mutableMapOf<Long, EntityView>()
```

Add to `render(flock)`, after the chicken loop:

```kotlin
        val liveChickIds = flock.chicks.map { it.id }.toSet()
        chickViews.keys.filterNot { it in liveChickIds }.forEach { staleId ->
            chickViews.remove(staleId)?.remove()
        }

        flock.chicks.forEach { chick ->
            val view = chickViews.getOrPut(chick.id) {
                EntityView(context, windowManager, com.juleah.chickens.sim.ChickConfig.SIZE_PX).also {
                    it.imageView.setOnClickListener { _ -> chick.runAway() }
                }
            }
            view.imageView.setImageBitmap(chickSprite.frame(chickFrameIndex(chick)))
            view.show(chick.x, chick.y)
        }
```

Add a private helper below `chickenFrameIndex`:

```kotlin
    private fun chickFrameIndex(chick: com.juleah.chickens.sim.ChickEntity): Int =
        when (chick.animState) {
            com.juleah.chickens.sim.ChickAnimState.IDLE -> 0
            com.juleah.chickens.sim.ChickAnimState.WALKING -> 2
            com.juleah.chickens.sim.ChickAnimState.RUNNING -> 2
        }
```

Extend `clear()` to also clear chick views:

```kotlin
    fun clear() {
        chickenViews.values.forEach { it.remove() }
        chickenViews.clear()
        chickViews.values.forEach { it.remove() }
        chickViews.clear()
    }
```

- [ ] **Step 3: Modify `OverlayService.kt` — tick chicks each frame**

In `tickRunnable.run()`, after the chicken `wander()` loop, add:

```kotlin
            flock.chicks.forEach { chick ->
                val parent = chick.parentId?.let { pid -> flock.chickens.find { it.id == pid } }
                if (parent != null) {
                    chick.followParent(TICK_INTERVAL_MS, parent.x, parent.y, screenWidthPx, screenHeightPx)
                } else {
                    chick.wanderAlone(TICK_INTERVAL_MS, screenWidthPx, screenHeightPx)
                }
            }
```

- [ ] **Step 4: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Manual verify**

Since chicks only exist after an egg hatches (Task 14), defer full visual verification to Task 14's check. For now just confirm the build succeeds and nothing regresses in Task 12's chicken behavior.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/juleah/chickens/sim/ChickEntity.kt app/src/main/java/com/juleah/chickens/render/OverlayRenderer.kt app/src/main/java/com/juleah/chickens/OverlayService.kt
git commit -m "feat: add chick rendering and parent-follow/run-away movement"
```

---

## Task 14: Egg laying, sitting, hatching, and rendering

**Files:**
- Modify: `app/src/main/java/com/juleah/chickens/sim/Flock.kt`
- Modify: `app/src/main/java/com/juleah/chickens/render/OverlayRenderer.kt`
- Modify: `app/src/main/java/com/juleah/chickens/OverlayService.kt`

**Interfaces:**
- Produces (additions to `Flock`): `fun maybeLayEgg(chicken: ChickenEntity): EggEntity?`, `fun maybeSitOnEgg(chicken: ChickenEntity): EggEntity?`.

- [ ] **Step 1: Modify `Flock.kt` — add egg-lay and egg-sit rolls**

Add a field alongside the existing lists:

```kotlin
    private val lastEggTimeByChickenId = mutableMapOf<Long, Long>()
```

Add methods (anywhere inside the class body, e.g. after `layEgg`):

```kotlin
    fun maybeLayEgg(chicken: ChickenEntity): EggEntity? {
        if (!canLayEgg()) return null
        val now = clock.nowMs()
        val last = lastEggTimeByChickenId[chicken.id] ?: 0L
        if (now - last < ChickenConfig.EGG_COOLDOWN_MS) return null
        if (rng.nextDouble() >= ChickenConfig.EGG_LAY_CHANCE) return null
        lastEggTimeByChickenId[chicken.id] = now
        return layEgg(chicken.id, chicken.x, chicken.y)
    }

    fun maybeSitOnEgg(chicken: ChickenEntity): EggEntity? {
        val candidate = eggs.find { egg ->
            egg.state == EggState.WAITING &&
                egg.sittingChickenIdOrNull() == null &&
                distanceBetween(chicken.x, chicken.y, egg.x, egg.y) <= ChickenConfig.EGG_SIT_DISTANCE_PX
        } ?: return null
        if (rng.nextDouble() >= ChickenConfig.EGG_SIT_CHANCE) return null
        candidate.startSitting(chicken.id, clock.nowMs())
        return candidate
    }

    private fun distanceBetween(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val dx = x1 - x2
        val dy = y1 - y2
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
```

- [ ] **Step 2: Modify `OverlayRenderer.kt` — add egg rendering**

Add a field:

```kotlin
    private val eggSprite = SpriteSheet(context, R.drawable.egg, com.juleah.chickens.sim.EggConfig.SIZE_PX)
    private val eggViews = mutableMapOf<Long, EntityView>()
```

Add to `render(flock)`, after the chick loop:

```kotlin
        val liveEggIds = flock.eggs.map { it.id }.toSet()
        eggViews.keys.filterNot { it in liveEggIds }.forEach { staleId ->
            eggViews.remove(staleId)?.remove()
        }

        flock.eggs.forEach { egg ->
            val view = eggViews.getOrPut(egg.id) {
                EntityView(context, windowManager, com.juleah.chickens.sim.EggConfig.SIZE_PX)
            }
            val frameIndex = if (egg.state == com.juleah.chickens.sim.EggState.HATCHING) 1 else 0
            view.imageView.setImageBitmap(eggSprite.frame(frameIndex))
            view.show(egg.x, egg.y)
        }
```

Extend `clear()`:

```kotlin
        eggViews.values.forEach { it.remove() }
        eggViews.clear()
```

- [ ] **Step 3: Modify `OverlayService.kt` — wire egg-lay/sit rolls and hatch confirmation**

In `tickRunnable.run()`, inside the chicken `wander()` loop (from Task 12), after `chicken.wander(...)`, add:

```kotlin
                flock.maybeSitOnEgg(chicken)
                flock.maybeLayEgg(chicken)
```

After the chick-follow loop (from Task 13), add:

```kotlin
            flock.eggsReadyToHatch().forEach { egg -> flock.confirmHatch(egg.id) }
```

- [ ] **Step 4: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Manual verify — full lay/sit/hatch/grow cycle**

Temporarily lower `ChickenConfig.EGG_COOLDOWN_MS` to something small (e.g. `2000L`) and raise `ChickenConfig.EGG_LAY_CHANCE`/`EGG_SIT_CHANCE` (e.g. to `0.05`) so the cycle is observable in a couple of minutes instead of waiting on the real probabilities; also consider the Task 11 lifespan/growth-time shortcuts. Rebuild, start the overlay, and confirm: a chicken lays an egg (egg sprite appears), a chicken sits on it (egg sprite may switch to the "hatching" frame after the sitting timer), a chick appears near the egg's position and follows the nearest chicken, and the chick eventually turns into a full chicken (Task 13). Revert all temporary config changes before committing.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/juleah/chickens/sim/Flock.kt app/src/main/java/com/juleah/chickens/render/OverlayRenderer.kt app/src/main/java/com/juleah/chickens/OverlayService.kt
git commit -m "feat: add egg laying, sitting, hatching, and rendering"
```

---

## Task 15: Feed bag and feed piles

**Files:**
- Create: `app/src/main/java/com/juleah/chickens/sim/FeedPile.kt`
- Test: `app/src/test/java/com/juleah/chickens/sim/FeedPileTest.kt`
- Modify: `app/src/main/java/com/juleah/chickens/sim/Flock.kt`
- Create: `app/src/main/java/com/juleah/chickens/render/FeedBagView.kt`
- Modify: `app/src/main/java/com/juleah/chickens/render/OverlayRenderer.kt`
- Modify: `app/src/main/java/com/juleah/chickens/OverlayService.kt`

**Interfaces:**
- Consumes: `FeedConfig` (Task 2), `OverlayPermission.overlayWindowType()` (Task 8).
- Produces: `FeedPile(id: Long, x: Double, y: Double)` with `val amount: Int`, `val isEmpty: Boolean`, `fun consume(amount: Int = FeedConfig.CONSUME_AMOUNT): Boolean`; `Flock` additions `val feedPiles: MutableList<FeedPile>`, `fun placeFeed(x: Double, y: Double): FeedPile`, `fun removeEmptyFeedPiles()`; `FeedBagView(context, windowManager, sizePx, onDrop: (Double, Double) -> Unit)` with `fun show(dockX: Int, dockY: Int)`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.juleah.chickens.sim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedPileTest {

    @Test
    fun `starts at initial amount`() {
        val pile = FeedPile(id = 1, x = 0.0, y = 0.0)
        assertEquals(FeedConfig.INITIAL_AMOUNT, pile.amount)
        assertFalse(pile.isEmpty)
    }

    @Test
    fun `consume reduces amount and reports empty at zero`() {
        val pile = FeedPile(id = 1, x = 0.0, y = 0.0)
        var empty = false
        repeat(FeedConfig.INITIAL_AMOUNT / FeedConfig.CONSUME_AMOUNT) {
            empty = pile.consume()
        }
        assertTrue(empty)
        assertEquals(0, pile.amount)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.juleah.chickens.sim.FeedPileTest"`
Expected: FAIL to compile — `FeedPile` is unresolved.

- [ ] **Step 3: Create `FeedPile.kt`**

```kotlin
package com.juleah.chickens.sim

class FeedPile(
    val id: Long,
    val x: Double,
    val y: Double
) {
    var amount: Int = FeedConfig.INITIAL_AMOUNT
        private set
    val isEmpty: Boolean get() = amount <= 0

    fun consume(amount: Int = FeedConfig.CONSUME_AMOUNT): Boolean {
        this.amount = (this.amount - amount).coerceAtLeast(0)
        return isEmpty
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.juleah.chickens.sim.FeedPileTest"`
Expected: PASS

- [ ] **Step 5: Modify `Flock.kt` — add feed pile management**

Add a field alongside `eggs`:

```kotlin
    val feedPiles: MutableList<FeedPile> = mutableListOf()
```

Add methods (e.g. after `confirmHatch`):

```kotlin
    fun placeFeed(x: Double, y: Double): FeedPile {
        val pile = FeedPile(id = newId(), x = x, y = y)
        feedPiles.add(pile)
        return pile
    }

    fun removeEmptyFeedPiles() {
        feedPiles.removeAll { it.isEmpty }
    }
```

- [ ] **Step 6: Create `FeedBagView.kt`**

```kotlin
package com.juleah.chickens.render

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.ImageView
import com.juleah.chickens.overlay.OverlayPermission

class FeedBagView(
    context: Context,
    private val windowManager: WindowManager,
    sizePx: Int,
    private val onDrop: (x: Double, y: Double) -> Unit
) {
    val imageView = ImageView(context)
    private val layoutParams = WindowManager.LayoutParams(
        sizePx,
        sizePx,
        OverlayPermission.overlayWindowType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private var dockX = 0
    private var dockY = 0
    private var touchStartRawX = 0f
    private var touchStartRawY = 0f
    private var layoutStartX = 0
    private var layoutStartY = 0

    fun show(dockX: Int, dockY: Int) {
        this.dockX = dockX
        this.dockY = dockY
        layoutParams.x = dockX
        layoutParams.y = dockY
        windowManager.addView(imageView, layoutParams)
        imageView.setOnTouchListener { _, event -> handleTouch(event) }
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartRawX = event.rawX
                touchStartRawY = event.rawY
                layoutStartX = layoutParams.x
                layoutStartY = layoutParams.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                layoutParams.x = layoutStartX + (event.rawX - touchStartRawX).toInt()
                layoutParams.y = layoutStartY + (event.rawY - touchStartRawY).toInt()
                windowManager.updateViewLayout(imageView, layoutParams)
                return true
            }
            MotionEvent.ACTION_UP -> {
                val movedX = event.rawX - touchStartRawX
                val movedY = event.rawY - touchStartRawY
                if (kotlin.math.abs(movedX) > 24 || kotlin.math.abs(movedY) > 24) {
                    onDrop(layoutParams.x.toDouble(), layoutParams.y.toDouble())
                }
                layoutParams.x = dockX
                layoutParams.y = dockY
                windowManager.updateViewLayout(imageView, layoutParams)
                return true
            }
        }
        return false
    }
}
```

- [ ] **Step 7: Modify `OverlayRenderer.kt` — add feed pile rendering**

Add a field:

```kotlin
    private val feedSprite = SpriteSheet(context, R.drawable.feed, com.juleah.chickens.sim.FeedConfig.SIZE_PX)
    private val feedPileViews = mutableMapOf<Long, EntityView>()
```

Add to `render(flock)`, after the egg loop:

```kotlin
        val livePileIds = flock.feedPiles.map { it.id }.toSet()
        feedPileViews.keys.filterNot { it in livePileIds }.forEach { staleId ->
            feedPileViews.remove(staleId)?.remove()
        }

        flock.feedPiles.forEach { pile ->
            val view = feedPileViews.getOrPut(pile.id) {
                EntityView(context, windowManager, com.juleah.chickens.sim.FeedConfig.SIZE_PX)
            }
            view.imageView.setImageBitmap(feedSprite.frame(feedFrameIndex(pile)))
            view.show(pile.x, pile.y)
        }
```

Add a private helper:

```kotlin
    private fun feedFrameIndex(pile: com.juleah.chickens.sim.FeedPile): Int {
        val fraction = pile.amount.toDouble() / com.juleah.chickens.sim.FeedConfig.INITIAL_AMOUNT
        return when {
            fraction > com.juleah.chickens.sim.FeedConfig.FULL_FRAME_THRESHOLD -> 0
            fraction > com.juleah.chickens.sim.FeedConfig.HALF_FRAME_THRESHOLD -> 1
            else -> 2
        }
    }
```

Extend `clear()`:

```kotlin
        feedPileViews.values.forEach { it.remove() }
        feedPileViews.clear()
```

- [ ] **Step 8: Modify `OverlayService.kt` — feed bag, feed-seeking, and consumption**

Add imports `import com.juleah.chickens.render.FeedBagView` and a field:

```kotlin
    private lateinit var feedBagView: FeedBagView
```

At the end of `onCreate()`, after creating `renderer`:

```kotlin
        val dockSizePx = 48
        feedBagView = FeedBagView(this, windowManager, dockSizePx) { x, y -> flock.placeFeed(x, y) }
        feedBagView.imageView.setImageBitmap(
            android.graphics.BitmapFactory.decodeResource(resources, R.drawable.feed_bag)
        )
        feedBagView.show(
            dockX = (screenWidthPx - dockSizePx - 24).toInt(),
            dockY = (screenHeightPx - dockSizePx - 24).toInt()
        )
```

Replace the chicken `wander()`/egg-roll loop in `tickRunnable.run()` with:

```kotlin
            val feedNotifyRadiusPx = minOf(screenWidthPx, screenHeightPx) * com.juleah.chickens.sim.FeedConfig.NOTIFY_RADIUS_FRACTION
            flock.chickens.forEach { chicken ->
                val targetId = chicken.currentTargetFeedId()
                val targetPile = targetId?.let { id -> flock.feedPiles.find { it.id == id } }
                when {
                    targetPile != null -> {
                        val reached = chicken.moveTowardFeed(TICK_INTERVAL_MS, targetPile.x, targetPile.y)
                        if (reached && targetPile.consume()) chicken.clearTargetFeed()
                    }
                    else -> {
                        val nearby = flock.feedPiles.minByOrNull { distanceBetween(chicken.x, chicken.y, it.x, it.y) }
                        if (nearby != null && distanceBetween(chicken.x, chicken.y, nearby.x, nearby.y) <= feedNotifyRadiusPx) {
                            chicken.rushToFeed(nearby.id)
                        } else {
                            chicken.wander(TICK_INTERVAL_MS, screenWidthPx, screenHeightPx)
                        }
                    }
                }
                flock.maybeSitOnEgg(chicken)
                flock.maybeLayEgg(chicken)
            }
```

Add the helper below `buildNotification()`:

```kotlin
    private fun distanceBetween(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val dx = x1 - x2
        val dy = y1 - y2
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
```

After the `flock.eggsReadyToHatch()...` line, add:

```kotlin
            flock.removeEmptyFeedPiles()
```

- [ ] **Step 9: Build**

Run: `./gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 10: Manual verify — feed drag and consumption**

Start the overlay. Expected: a feed bag icon sits docked in the bottom-right corner. Press and drag it elsewhere on screen, release — a feed pile appears at the drop point and the bag springs back to its corner. Nearby chickens should turn and move toward the pile, peck at it, and the pile should visually shrink (sprite frame changes) and eventually disappear.

- [ ] **Step 11: Commit**

```bash
git add app/src/main/java/com/juleah/chickens/sim/FeedPile.kt app/src/test/java/com/juleah/chickens/sim/FeedPileTest.kt app/src/main/java/com/juleah/chickens/sim/Flock.kt app/src/main/java/com/juleah/chickens/render/FeedBagView.kt app/src/main/java/com/juleah/chickens/render/OverlayRenderer.kt app/src/main/java/com/juleah/chickens/OverlayService.kt
git commit -m "feat: add draggable feed bag and feed pile consumption"
```

---

## Task 16: Device verification and README

**Files:**
- Create: `README.md`

- [ ] **Step 1: Full-cycle soak test on two API levels**

On a KitKat (API 19) emulator/device and a modern (API 34, or your newest available) emulator/device:
1. Install and launch, confirm the overlay permission flow matches the SDK branch (silent grant on API 19, settings-screen prompt on the modern device).
2. Start the overlay, confirm chickens render and wander, and confirm you can still open and use another app underneath them (touch passthrough).
3. Drag-drop the feed bag, confirm chickens converge and the pile depletes.
4. Let it run — with the temporary config shortcuts from Tasks 11/14 if you want to speed this up, reverted before this final check — long enough to observe at least one full lay → sit → hatch → grow → death → auto-respawn cycle without a crash. `adb logcat | grep -i chickens` should show no exceptions.
5. Tap Stop, confirm the notification (API 26+) and all overlay windows disappear.

- [ ] **Step 2: Create `README.md`**

```markdown
# Chickens

Android overlay chicken sim — draws over the home screen and other apps.
Based on the browser sim at mombotro.

## Build

    ./gradlew assembleDebug

## Install

    ./gradlew installDebug

## Run

Launch "Chickens" from the app drawer, tap "Start Chickens". On Android 6.0+
this opens the "draw over other apps" permission screen — enable it and
return to the app; the overlay starts automatically. Tap "Stop Chickens" to
stop it, or:

    adb shell am force-stop com.juleah.chickens

## Known simplifications vs. the browser version

- Chicken jump freezes in place for the jump duration rather than animating
  an arc.
- Egg hatching is a single state transition rather than mombotro's
  multi-frame shell-crack animation.
- Feed is placed by dragging the feed bag icon to a spot on screen, not by
  clicking the bag then clicking the ground — an overlay has no full-screen
  touchable surface to catch a "click anywhere" gesture without blocking
  every app underneath it.
```

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: add README with build/run instructions and known simplifications"
```
