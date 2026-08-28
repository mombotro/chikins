package com.juleah.chickens

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.juleah.chickens.overlay.OverlayPermission
import com.juleah.chickens.sim.PopulationConfig

class MainActivity : Activity() {

    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1001
        private const val PREFS_NAME = "chikins_prefs"
        private const val PREF_MAX_POPULATION = "max_population"
        // SeekBar progress 0-49 maps to max population 1-50.
        private const val MAX_POPULATION_FLOOR = 1
    }

    private var isServiceRunning = false
    private lateinit var statusText: TextView
    private lateinit var toggleButton: Button
    private lateinit var maxChikinSeekBar: SeekBar
    private lateinit var maxChikinLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        toggleButton = findViewById(R.id.toggleButton)
        maxChikinLabel = findViewById(R.id.maxChikinLabel)
        maxChikinSeekBar = findViewById(R.id.maxChikinSeekBar)
        val helpButton = findViewById<Button>(R.id.helpButton)
        val killAllButOneButton = findViewById<Button>(R.id.killAllButOneButton)

        val sillyTypeface = Typeface.createFromAsset(assets, "fonts/silly.ttf")
        statusText.typeface = sillyTypeface
        toggleButton.typeface = sillyTypeface
        helpButton.typeface = sillyTypeface
        maxChikinLabel.typeface = sillyTypeface
        killAllButOneButton.typeface = sillyTypeface

        toggleButton.setOnClickListener {
            if (isServiceRunning) stopOverlay() else startOverlayOrRequestPermission()
        }
        helpButton.setOnClickListener { showHelp() }
        killAllButOneButton.setOnClickListener { killAllButOne() }

        val savedMax = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getInt(PREF_MAX_POPULATION, PopulationConfig.MAX_POPULATION)
        PopulationConfig.MAX_POPULATION = savedMax
        maxChikinSeekBar.progress = savedMax - MAX_POPULATION_FLOOR
        updateMaxChikinLabel(savedMax)

        maxChikinSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val newMax = progress + MAX_POPULATION_FLOOR
                PopulationConfig.MAX_POPULATION = newMax
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                    .putInt(PREF_MAX_POPULATION, newMax)
                    .apply()
                updateMaxChikinLabel(newMax)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        updateUi()
    }

    private fun updateMaxChikinLabel(max: Int) {
        maxChikinLabel.text = "${getString(R.string.max_chikins_label)}: $max"
    }

    private fun killAllButOne() {
        val service = OverlayService.instance
        if (service == null) {
            Toast.makeText(this, "start chikins first", Toast.LENGTH_SHORT).show()
            return
        }
        service.flock.killAllButOne()
    }

    @Suppress("DEPRECATION") // Html.fromHtml(String,Int) needs API24+; this app's minSdk is 19.
    private fun showHelp() {
        val message = Html.fromHtml(
            "This draws over other apps.<br><br>" +
                "<b>how to:</b><br>" +
                "drag and drop the feedbag.<br>" +
                "tap the feedbag and anywhere on the screen to feed chikins.<br><br>" +
                "<a href=\"https://boccbo.cc\">boccbo.cc</a><br>" +
                "<a href=\"https://github.com/mombotro/mombotro.github.io\">mombotro</a>"
        )
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.help_title)
            .setMessage(message)
            .setPositiveButton("ok", null)
            .show()
        dialog.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethod.getInstance()
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
