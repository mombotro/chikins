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
