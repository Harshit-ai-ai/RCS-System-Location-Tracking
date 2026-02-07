package com.family.safetysms

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.family.safetysms.location.LocationHelper
import androidx.appcompat.app.AlertDialog
import androidx.activity.OnBackPressedCallback

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestIgnoreBatteryOptimizations()
        startLocationTracking()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitWarning()
                }
            }
        )
    }


    private fun showExitWarning() {

        AlertDialog.Builder(this)
                .setTitle("Exit Safety App?")
                .setMessage(
                        "This app is responsible for emergency location sharing.\n\n" +
                                "Closing it may stop SMS-based tracking."
                )
                .setPositiveButton("Exit") { _, _ ->
                    finish()
                }
                .setNegativeButton("Keep Running", null)
                .setCancelable(true)
                .show()
    }

    private fun startLocationTracking() {
        val locationHelper = LocationHelper(this)

        locationHelper.getLocation { latitude, longitude ->
            Log.d("LOCATION", "Lat=$latitude, Lon=$longitude")
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager

        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Disable battery optimization manually for this app",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}