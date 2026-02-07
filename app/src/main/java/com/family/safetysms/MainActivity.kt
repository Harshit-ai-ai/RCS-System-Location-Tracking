package com.family.safetysms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var startButton: Button

    private val REQ_SMS = 101
    private val REQ_LOCATION = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)

        updateStatus()

        startButton.setOnClickListener {
            startProtectionFlow()
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitWarning()
                }
            }
        )
    }

    private fun startProtectionFlow() {
        if (!hasSmsPermission()) {
            explainSmsPermission()
            return
        }

        if (!hasLocationPermission()) {
            explainLocationPermission()
            return
        }

        explainBatteryOptimization()
    }

    private fun explainSmsPermission() {
        AlertDialog.Builder(this)
            .setTitle("SMS Access Needed")
            .setMessage(
                "This allows your phone to read a special emergency message " +
                        "sent by your family.\n\n" +
                        "Without this, we cannot respond to them."
            )
            .setPositiveButton("Continue") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.SEND_SMS
                    ),
                    REQ_SMS
                )
            }
            .setCancelable(false)
            .show()
    }

    private fun explainLocationPermission() {
        AlertDialog.Builder(this)
            .setTitle("Location Access Needed")
            .setMessage(
                "This allows your family to find you if you are lost or need help.\n\n" +
                        "Your location is shared ONLY when they request it."
            )
            .setPositiveButton("Continue") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ),
                    REQ_LOCATION
                )
            }
            .setCancelable(false)
            .show()
    }

    private fun explainBatteryOptimization() {
        AlertDialog.Builder(this)
            .setTitle("Final Important Step")
            .setMessage(
                "Android may stop this app to save battery.\n\n" +
                        "Please allow it to run all the time so it works in emergencies."
            )
            .setPositiveButton("Allow") { _, _ ->
                requestIgnoreBatteryOptimizations()
                Toast.makeText(
                    this,
                    "Protection is now active",
                    Toast.LENGTH_LONG
                ).show()
                updateStatus()
            }
            .setCancelable(false)
            .show()
    }

    private fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestIgnoreBatteryOptimizations() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun updateStatus() {
        val sms = if (hasSmsPermission()) "✔" else "✖"
        val loc = if (hasLocationPermission()) "✔" else "✖"

        statusText.text =
            "$sms SMS Access\n" +
                    "$loc Location Access\n\n" +
                    "Send 'LOC' from a trusted number to request location"
    }

    private fun showExitWarning() {
        AlertDialog.Builder(this)
            .setTitle("Exit Safety App?")
            .setMessage(
                "This app protects you in emergencies.\n\n" +
                        "Closing it may stop SMS-based location sharing."
            )
            .setPositiveButton("Exit") { _, _ -> finish() }
            .setNegativeButton("Keep Running", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.any { it != PackageManager.PERMISSION_GRANTED }) {
            Toast.makeText(
                this,
                "Permission denied. Protection is incomplete.",
                Toast.LENGTH_LONG
            ).show()
        }

        updateStatus()
    }
}