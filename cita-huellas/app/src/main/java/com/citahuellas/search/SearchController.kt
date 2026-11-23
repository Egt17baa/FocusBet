package com.citahuellas.search

import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.citahuellas.data.AppSettings
import com.citahuellas.data.Profile
import com.citahuellas.engine.Action
import com.citahuellas.engine.PageClassifier
import com.citahuellas.engine.PageKind
import com.citahuellas.engine.SearchEngine
import com.citahuellas.web.JsBridge
import com.citahuellas.web.parseSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.random.Random
import kotlinx.serialization.builtins.serializer

enum class StatusColor { IDLE, SEARCHING, FOUND, STOPPED }

data class SearchUiState(
    val status: String = "Elige una persona y pulsa Buscar cita",
    val attempt: Int = 0,
    val max: Int = 0,
    val secondsLeft: Int? = null,
    val color: StatusColor = StatusColor.IDLE,
    val smsStep: Boolean = false,
) {
    val active get() = color == StatusColor.SEARCHING || color == StatusColor.FOUND
}

const val ENTRY_URL = "https://icp.administracionelectronica.gob.es/icpplustiem/index.html"

/** Une WebView + motor: en cada página cargada espera 2–4 s, mira la pantalla y hace el paso que toca. */
class SearchController(
    private val web: WebView,
    private val bridgeJs: String,
    private val notifier: Notifier,
    private val scope: CoroutineScope,
    private val onSmsStep: () -> Unit,
    val diagnostics: PageDiagnostics = PageDiagnostics(),
) {
    var state by mutableStateOf(SearchUiState())
        private set

    private var engine: SearchEngine? = null
    private var profileName = ""
    private var listenSms = false
    private var job: Job? = null
    private var pageSeq = 0
    private var notifiedFound = false
    /** Durante la cuenta atrás se ignoran cargas tardías de la página "no hay citas". */
    private var waiting = false

    fun start(profile: Profile, phone: String, email: String, useMyPhone: Boolean, settings: AppSettings) {
        job?.cancel()
        engine = SearchEngine(profile, phone, email, settings)
        profileName = profile.fullName.substringBefore(' ')
        listenSms = useMyPhone
        notifiedFound = false
        state = SearchUiState("Abriendo la web…", 1, settings.maxAttempts, null, StatusColor.SEARCHING)
        web.loadUrl(ENTRY_URL)
    }

    /** Abre la web sin buscar, para comprobar a mano que carga. */
    fun openOnly() {
        cancel()
        state = SearchUiState("Abriendo la web sin buscar…")
        web.loadUrl(ENTRY_URL)
    }

    /** Borra cookies y almacenamiento de la web: la siguiente carga es como una visita nueva. */
    suspend fun clearWebData() {
        suspendCancellableCoroutine { cont ->
            android.webkit.CookieManager.getInstance().removeAllCookies { if (cont.isActive) cont.resume(Unit) }
        }
        android.webkit.CookieManager.getInstance().flush()
        android.webkit.WebStorage.getInstance().deleteAllData()
    }

    fun clearWebDataNow(onDone: () -> Unit) {
        scope.launch { clearWebData(); onDone() }
    }

    fun cancel() {
        job?.cancel()
        waiting = false
        engine = null
        state = SearchUiState("Búsqueda cancelada", color = StatusColor.IDLE)
    }

    /** Lo llama el WebViewClient. Si llegan varias cargas seguidas, solo cuenta la última. */
    fun onPageFinished() {
        diagnostics.event("terminada ${web.url}")
        val e = engine ?: return
        if (e.finished || waiting) return
        val seq = ++pageSeq
        job?.cancel()
        job = scope.launch {
            delay(Random.nextLong(1_000, 2_001))
            if (seq != pageSeq) return@launch
            step(e)
        }
    }

    /** Botón manual: hace el paso de la pantalla actual con una persona, sin reintentos. */
    fun doCurrentStep(profile: Profile, phone: String, email: String, settings: AppSettings) {
        scope.launch {
            val kind = currentKind() ?: return@launch
            val action = SearchEngine(profile, phone, email, settings).next(kind)
            JsBridge.js(action)?.let { eval(it) }
        }
    }

    fun fillSms(code: String) {
        scope.launch { eval(JsBridge.fillSmsCode(code)) }
    }

    private suspend fun currentKind(): PageKind? {
        eval(bridgeJs)
        val snap = parseSnapshot(eval(JsBridge.SNAPSHOT))
        diagnostics.onText(snap?.text)
        diagnostics.onPageDebug(runCatching { kotlinx.serialization.json.Json.decodeFromString<String>(eval("CH.debug()") ?: "null") }.getOrNull())
        return snap?.let(PageClassifier::classify)
    }

    private suspend fun step(e: SearchEngine) {
        val kind = currentKind() ?: PageKind.THROTTLED
        val action = e.next(kind)
        if (action is Action.Stop || action is Action.Found) {
            diagnostics.onControls(runCatching { kotlinx.serialization.json.Json.decodeFromString<String>(eval(JsBridge.CONTROLS) ?: "null") }.getOrNull())
        }
        if (action is Action.Stop) diagnostics.onStop(action.reason, kind.name)
        when (action) {
            is Action.WaitAndRestart -> waitAndRestart(action)
            is Action.RetryAfterError -> retryAfterError(action)
            is Action.WaitForPage -> {
                state = state.copy(status = "Esperando a que la web cargue…", secondsLeft = null)
                val seq = pageSeq
                delay(action.delayMs)
                if (seq == pageSeq) step(e) // si entretanto cargó otra página, ya se ocupa onPageFinished
            }
            is Action.Found -> found(action.kind)
            is Action.Stop -> stop(
                if (diagnostics.hasSslError()) "La app no puede verificar el certificado de seguridad de la web. Avisa para actualizar la app.\n${diagnostics.summary()}"
                else if (kind in setOf(PageKind.NETWORK_ERROR, PageKind.BLANK, PageKind.THROTTLED, PageKind.ERROR, PageKind.UNKNOWN)) "${action.reason}\n${diagnostics.summary()}" else action.reason
            )
            else -> {
                state = state.copy(status = label(action), secondsLeft = null)
                var missing = JsBridge.missingField(eval(JsBridge.js(action)!!))
                val followUp = JsBridge.followUp(action)
                if (missing == null && followUp != null) {
                    delay(Random.nextLong(1_500, 2_501))
                    missing = JsBridge.missingField(eval(followUp))
                }
                if (missing != null) {
                    diagnostics.onControls(runCatching { kotlinx.serialization.json.Json.decodeFromString<String>(eval(JsBridge.CONTROLS) ?: "null") }.getOrNull())
                    if (missing != "?") diagnostics.event("opciones de $missing: " + (eval("CH.options(" + kotlinx.serialization.json.Json.encodeToString(String.serializer(), missing) + ")") ?: "?"))
                    diagnostics.onStop("falta $missing", kind.name)
                    engine = null
                    stop("No encuentro ${JsBridge.fieldLabel(missing)} en la web. Sigue tú a mano.")
                }
            }
        }
    }

    private suspend fun waitAndRestart(a: Action.WaitAndRestart) {
        waiting = true
        try {
            var left = (a.delayMs / 1000).toInt()
            while (left > 0) {
                state = state.copy(status = "No hay citas. Reintento ${a.attempt} de ${state.max}", attempt = a.attempt, secondsLeft = left)
                delay(1_000)
                left--
            }
        } finally {
            waiting = false
        }
        state = state.copy(status = "Intento ${a.attempt} de ${state.max}…", secondsLeft = null)
        if (JsBridge.missingField(eval(JsBridge.ACCEPT_NO_APPOINTMENTS)) != null) web.loadUrl(ENTRY_URL)
    }

    private suspend fun retryAfterError(a: Action.RetryAfterError) {
        diagnostics.event("reintento por fallo de carga ${a.retry}/2")
        waiting = true
        try {
            var left = (a.delayMs / 1000).toInt()
            while (left > 0) {
                state = state.copy(status = "La web no respondió. Vuelvo a cargar (${a.retry}/2)", secondsLeft = left)
                delay(1_000)
                left--
            }
        } finally {
            waiting = false
        }
        if (a.clearCookies) {
            diagnostics.event("borrando cookies de la web antes de reintentar")
            clearWebData()
        }
        state = state.copy(status = "Cargando de nuevo…", secondsLeft = null)
        web.loadUrl(ENTRY_URL)
    }

    private fun found(kind: PageKind) {
        if (!notifiedFound) {
            notifiedFound = true
            notifier.found(profileName, sure = kind != PageKind.UNKNOWN && kind != PageKind.CAPTCHA)
        }
        val sms = kind == PageKind.SLOT_OR_SMS
        if (sms && listenSms && !state.smsStep) onSmsStep()
        state = state.copy(
            color = StatusColor.FOUND,
            secondsLeft = null,
            smsStep = sms,
            status = when (kind) {
                PageKind.UNKNOWN -> "¡Pantalla nueva tras pedir cita! Puede ser una cita: revísala tú YA."
                PageKind.CAPTCHA -> "¡Captcha tras pedir cita! Resuélvelo tú YA y sigue."
                PageKind.CONTACT -> "Teléfono y correo puestos. Revisa y pulsa Siguiente."
                PageKind.SLOT_OR_SMS -> "Elige el hueco y pon el código SMS. Confirma tú."
                else -> "¡Hay cita! Elige la oficina y pulsa Siguiente."
            },
        )
    }

    private fun stop(reason: String) {
        notifier.stopped(reason.substringBefore('\n'))
        state = state.copy(status = reason, color = StatusColor.STOPPED, secondsLeft = null)
    }

    private fun label(a: Action): String = when (a) {
        Action.SelectProvince -> "Intento ${state.attempt.coerceAtLeast(1)} de ${state.max}: provincia Madrid"
        Action.SelectProcedure -> "Eligiendo toma de huellas"
        Action.EnterWithoutClave -> "Presentación sin Cl@ve"
        is Action.FillPersonal -> "Rellenando datos de la persona"
        Action.Request -> "Solicitando cita…"
        is Action.FillContact -> "Poniendo teléfono y correo"
        else -> state.status
    }

    /** Si la página no responde en 15 s se trata como vacía (la búsqueda se para con diagnóstico, no se cuelga). */
    private suspend fun eval(js: String): String? = withTimeoutOrNull(15_000) {
        suspendCancellableCoroutine { cont ->
            web.evaluateJavascript(js) { if (cont.isActive) cont.resume(it) }
        }
    } ?: run { diagnostics.event("sin respuesta de la página (15 s)"); null }
}
