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
import com.juleah.chickens.render.OverlayRenderer
import com.juleah.chickens.sim.EggState
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

    private lateinit var renderer: OverlayRenderer
    private val tickHandler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var lastTickElapsedRealtimeMs = 0L

    private val tickRunnable = object : Runnable {
        override fun run() {
            val now = android.os.SystemClock.elapsedRealtime()
            // Real tick cost regularly runs 150-450ms on real hardware (WindowManager
            // view updates dominate), well above the 33ms target, so the clamp ceiling
            // must sit well above normal operating range or slower devices get their
            // movement silently under-credited. It exists only to bound truly abnormal
            // pauses (e.g. screen off for a while), not to model expected tick cost.
            val deltaMs = if (lastTickElapsedRealtimeMs == 0L) {
                TICK_INTERVAL_MS
            } else {
                (now - lastTickElapsedRealtimeMs).coerceAtMost(2000L)
            }
            lastTickElapsedRealtimeMs = now

            flock.tick()
            Log.d(TAG, "population=${flock.populationCount()} deltaMs=$deltaMs")

            val sittingEggByChickenId = flock.eggs
                .filter { it.state == EggState.SITTING }
                .mapNotNull { egg -> egg.sittingChickenIdOrNull()?.let { it to egg } }
                .toMap()

            flock.chickens.forEach { chicken ->
                val sittingEgg = sittingEggByChickenId[chicken.id]
                if (sittingEgg != null) {
                    chicken.holdSittingPose(sittingEgg.x, sittingEgg.y, deltaMs)
                } else {
                    chicken.wander(deltaMs, screenWidthPx, screenHeightPx)
                    flock.maybeSitOnEgg(chicken, deltaMs)
                    flock.maybeLayEgg(chicken, deltaMs)
                }
            }

            flock.chicks.forEach { chick ->
                val parent = chick.parentId?.let { pid -> flock.chickens.find { it.id == pid } }
                if (parent != null) {
                    chick.followParent(deltaMs, parent.x, parent.y, parent.facingRight, screenWidthPx, screenHeightPx)
                } else {
                    chick.wanderAlone(deltaMs, screenWidthPx, screenHeightPx)
                }
            }

            flock.eggsReadyToHatch().forEach { egg -> flock.confirmHatch(egg.id) }

            // Flock's own Clock (SystemClock, see sim/Clock.kt) uses wall-clock
            // System.currentTimeMillis() for all entity deadlines - deliberately
            // not the elapsedRealtime() used above for deltaMs, so this must match.
            renderer.render(flock, System.currentTimeMillis())
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

        renderer = OverlayRenderer(this, windowManager)
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
        renderer.clear()
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
