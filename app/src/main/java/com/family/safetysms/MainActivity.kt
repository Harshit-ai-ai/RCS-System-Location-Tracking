package com.family.safetysms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.provider.ContactsContract
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.family.safetysms.contacts.TrustedContactsManager

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var addContactButton: Button

    private val REQ_SMS = 101
    private val REQ_LOCATION = 102
    private val REQ_CONTACT = 103
    private val PICK_CONTACT = 201

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)
        addContactButton = findViewById(R.id.addContactButton)

        updateStatus()

        startButton.setOnClickListener {
            startProtectionFlow()
        }

        addContactButton.setOnClickListener {
            addTrustedContact()
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

    // 🔐 Guided protection flow
    private fun startProtectionFlow() {
        when {
            !hasSmsPermission() -> explainSmsPermission()
            !hasLocationPermission() -> explainLocationPermission()
            else -> explainBatteryOptimization()
        }
    }

    // 📇 Add trusted contact
    private fun addTrustedContact() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                REQ_CONTACT
            )
        } else {
            pickContact()
        }
    }

    private fun pickContact() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
        }
        startActivityForResult(intent, PICK_CONTACT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_CONTACT && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            val cursor = contentResolver.query(
                uri,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val number = it.getString(0)
                    TrustedContactsManager.addNumber(this, number)
                    Toast.makeText(
                        this,
                        "Trusted contact added",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun explainSmsPermission() {
        AlertDialog.Builder(this)
            .setTitle("SMS Permission Needed")
            .setMessage(
                "This lets your family send an emergency request.\n\n" +
                        "Only safety messages are read."
            )
            .setPositiveButton("Allow") { _, _ ->
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
            .setTitle("Location Permission Needed")
            .setMessage(
                "This helps your family find you if needed.\n\n" +
                        "Location is shared ONLY on request."
            )
            .setPositiveButton("Allow") { _, _ ->
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
            .setTitle("Final Step")
            .setMessage(
                "Please allow this app to run always.\n\n" +
                        "This ensures it works during emergencies."
            )
            .setPositiveButton("Allow") { _, _ ->
                requestIgnoreBatteryOptimizations()
                Toast.makeText(this, "Protection ACTIVE", Toast.LENGTH_LONG).show()
                updateStatus()
            }
            .setCancelable(false)
            .show()
    }

    private fun hasSmsPermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestIgnoreBatteryOptimizations() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun updateStatus() {
        val sms = if (hasSmsPermission()) "✔ SMS Access" else "✖ SMS Access"
        val loc = if (hasLocationPermission()) "✔ Location Access" else "✖ Location Access"

        statusText.text =
            "$sms\n$loc\n\n" +
                    "Only trusted contacts can request your location"
    }

    private fun showExitWarning() {
        AlertDialog.Builder(this)
            .setTitle("Exit Safety App?")
            .setMessage(
                "Closing this app may stop emergency protection."
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
                "Permission denied. Protection incomplete.",
                Toast.LENGTH_LONG
            ).show()
        }

        updateStatus()
    }
}