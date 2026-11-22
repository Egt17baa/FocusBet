package com.citahuellas.search

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

/**
 * SMS User Consent: Android pregunta "¿Permitir que Cita Huellas lea este mensaje?" para UN solo SMS.
 * La app no tiene permiso para leer el resto de mensajes.
 */
class SmsConsent(private val activity: ComponentActivity, private val onCode: (String) -> Unit) {
    private val launcher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
        extractSmsCode(message)?.let(onCode)
    }

    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != SmsRetriever.SMS_RETRIEVED_ACTION) return
            val extras = intent.extras ?: return
            @Suppress("DEPRECATION")
            val status = extras.get(SmsRetriever.EXTRA_STATUS) as? Status ?: return
            if (status.statusCode != CommonStatusCodes.SUCCESS) return
            val consent: Intent? = if (Build.VERSION.SDK_INT >= 33) {
                extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT, Intent::class.java)
            } else {
                @Suppress("DEPRECATION") extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT)
            }
            consent?.let { runCatching { launcher.launch(it) } }
        }
    }

    /** Escucha durante 5 minutos el siguiente SMS que contenga un código. */
    fun listen() {
        SmsRetriever.getClient(activity).startSmsUserConsent(null)
        if (!registered) {
            ContextCompat.registerReceiver(
                activity, receiver, IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
                SmsRetriever.SEND_PERMISSION, null, ContextCompat.RECEIVER_EXPORTED,
            )
            registered = true
        }
    }

    fun release() {
        if (registered) runCatching { activity.unregisterReceiver(receiver) }
        registered = false
    }
}
