package com.family.safetysms.service

import android.app.*
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class LocationService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        startForeground(1, notification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val sender = intent?.getStringExtra("sender") ?: stopSelf().let { return START_NOT_STICKY }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000
        ).setMaxUpdates(1).build()

        fusedClient.requestLocationUpdates(
            request,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc: Location = result.lastLocation ?: return

                    val msg =
                        "Location:\nLat: ${loc.latitude}\nLon: ${loc.longitude}\n" +
                                "https://maps.google.com/?q=${loc.latitude},${loc.longitude}"

                    SmsManager.getDefault().sendTextMessage(sender, null, msg, null, null)
                    stopSelf()
                }
            },
            Looper.getMainLooper()
        )

        return START_NOT_STICKY
    }

    private fun notification(): Notification {
        val channelId = "location_channel"

        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                channelId,
                "Location Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Family Safety")
            .setContentText("Fetching location via SMS")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}