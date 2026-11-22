package com.citahuellas

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.citahuellas.data.AppSettings
import com.citahuellas.data.Crypto
import com.citahuellas.data.Profile
import com.citahuellas.data.ProfileStore
import com.citahuellas.data.SettingsStore
import com.citahuellas.data.keystoreKey
import com.citahuellas.search.Notifier
import com.citahuellas.search.SearchController
import com.citahuellas.search.SmsConsent
import com.citahuellas.ui.App
import com.citahuellas.ui.AppCallbacks
import com.citahuellas.ui.contactFor
import com.citahuellas.ui.theme.CitaTheme
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var profiles: ProfileStore
    private lateinit var settingsStore: SettingsStore
    private lateinit var controller: SearchController
    private lateinit var sms: SmsConsent
    private lateinit var web: WebView

    private var people by mutableStateOf(emptyList<Profile>())
    private var settings by mutableStateOf(AppSettings())
    private var smsCode by mutableStateOf("")

    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val crypto = Crypto { keystoreKey() }
        profiles = ProfileStore(File(filesDir, "profiles.bin"), crypto)
        settingsStore = SettingsStore(File(filesDir, "settings.bin"), crypto)
        people = runCatching { profiles.load() }.getOrDefault(emptyList())
        settings = runCatching { settingsStore.load() }.getOrDefault(AppSettings())

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.setGeolocationEnabled(false)
            setBackgroundColor(Color.WHITE)
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) =
                    controller.diagnostics.onLoad(url)
                override fun onPageFinished(view: WebView, url: String) = controller.onPageFinished()
                override fun onReceivedSslError(view: WebView, handler: android.webkit.SslErrorHandler, error: android.net.http.SslError) {
                    // Solo se registra para el diagnóstico; la carga se cancela como hace Android por defecto.
                    controller.diagnostics.onSslError(
                        error.primaryError, error.url,
                        error.certificate?.issuedTo?.dName ?: "?", error.certificate?.issuedBy?.dName ?: "?",
                    )
                    handler.cancel()
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) controller.diagnostics.onError(error.errorCode, error.description.toString())
                }
                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                    if (request.isForMainFrame) controller.diagnostics.onHttpError(response.statusCode)
                }
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    controller.diagnostics.event("navega ${request.url}${if (request.isRedirect) " (redirección)" else ""}")
                    val blocked = request.url.host?.endsWith("administracionelectronica.gob.es") != true
                    if (blocked && request.isForMainFrame) controller.diagnostics.onBlocked(request.url.toString())
                    return blocked
                }
            }
        }
        sms = SmsConsent(this) { code ->
            smsCode = code
            controller.fillSms(code)
        }
        controller = SearchController(
            web, assets.open("bridge.js").bufferedReader().use { it.readText() },
            Notifier(this), lifecycleScope, onSmsStep = { sms.listen() },
        )

        if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)

        setContent {
            CitaTheme {
                val search = controller.state
                LaunchedEffect(search.active) {
                    if (search.active) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                App(
                    people, settings, search, smsCode, web,
                    AppCallbacks(
                        savePerson = { profiles.upsert(it); people = profiles.load() },
                        deletePerson = { profiles.delete(it); people = profiles.load() },
                        saveSettings = { settingsStore.save(it); settings = settingsStore.load() },
                        start = { p, mine ->
                            val c = contactFor(p, mine, settings)
                            smsCode = ""
                            controller.start(p, c.phone, c.email, mine, settings)
                        },
                        cancel = { controller.cancel() },
                        openWeb = { controller.openOnly() },
                        copyReport = {
                            val cm = getSystemService(android.content.ClipboardManager::class.java)
                            cm.setPrimaryClip(android.content.ClipData.newPlainText("diagnóstico", controller.diagnostics.fullReport()))
                            android.widget.Toast.makeText(this, "Diagnóstico copiado", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        step = { p, mine -> val c = contactFor(p, mine, settings); controller.doCurrentStep(p, c.phone, c.email, settings) },
                        fillSms = { controller.fillSms(it) },
                        setSmsCode = { smsCode = it },
                    ),
                )
            }
        }
    }

    override fun onDestroy() {
        sms.release()
        web.destroy()
        super.onDestroy()
    }
}
