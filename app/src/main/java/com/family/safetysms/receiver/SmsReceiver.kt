package com.family.safetysms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsMessage
import com.family.safetysms.auth.AuthManager
import com.family.safetysms.contacts.TrustedContactsManager

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val bundle: Bundle = intent.extras ?: return
        val pdus = bundle["pdus"] as? Array<*> ?: return
        val format = bundle.getString("format")

        for (pdu in pdus) {

            val message = if (android.os.Build.VERSION.SDK_INT >= 23) {
                SmsMessage.createFromPdu(pdu as ByteArray, format)
            } else {
                SmsMessage.createFromPdu(pdu as ByteArray)
            }

            val sender = message.originatingAddress ?: continue

            // 🚫 Ignore untrusted numbers
            if (!TrustedContactsManager.isTrusted(context, sender)) {
                continue
            }

            val body = message.messageBody
                ?.trim()
                ?.replace(Regex("\\s+"), " ")
                ?: continue

            // Step 1️⃣ LOC command
            if (body.equals("LOC", ignoreCase = true)) {
                AuthManager.startAuth(sender, context)
                abortBroadcast()
                return
            }

            // Step 2️⃣ AUTH <code>
            if (body.startsWith("AUTH", ignoreCase = true)) {
                val parts = body.split(" ")

                if (parts.size == 2) {
                    AuthManager.verify(sender, parts[1], context)
                    abortBroadcast()
                    return
                }
            }
        }
    }
}