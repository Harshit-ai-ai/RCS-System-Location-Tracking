package com.family.safetysms.auth

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.telephony.SmsManager
import com.family.safetysms.service.LocationService

object AuthManager {

    private const val TIMEOUT = 10 * 60 * 1000L

    private fun normalize(num: String): String =
        num.replace("+91", "").takeLast(10)

    fun startAuth(sender: String, context: Context) {

        val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        val code = (1000..9999).random().toString()

        prefs.edit()
            .putString("number", normalize(sender))
            .putString("code", code)
            .putLong("time", System.currentTimeMillis())
            .putBoolean("used", false)
            .apply()

        SmsManager.getDefault().sendTextMessage(
            sender,
            null,
            "Reply with: AUTH $code (valid 2 min)",
            null,
            null
        )
    }

    fun verify(sender: String, entered: String, context: Context) {

        val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

        val savedNumber = prefs.getString("number", null) ?: return
        val savedCode = prefs.getString("code", null) ?: return
        val time = prefs.getLong("time", 0)
        val used = prefs.getBoolean("used", false)

        if (used) return
        if (normalize(sender) != savedNumber) return

        if (System.currentTimeMillis() - time > TIMEOUT) {
            clear(prefs)
            return
        }
        SmsManager.getDefault().sendTextMessage(
            sender, null,
            "AUTH OK. Fetching location...",
            null, null
        )

        if (entered == savedCode) {

            prefs.edit().putBoolean("used", true).apply()

            val intent = Intent(context, LocationService::class.java)
            intent.putExtra("sender", sender)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            clear(prefs)
        }
    }

    private fun clear(prefs: SharedPreferences) {
        prefs.edit().clear().apply()
    }
}