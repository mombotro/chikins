package com.juleah.chickens

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
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

        val sillyTypeface = Typeface.createFromAsset(assets, "fonts/silly.ttf")
        statusText.typeface = sillyTypeface
        toggleButton.typeface = sillyTypeface

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
