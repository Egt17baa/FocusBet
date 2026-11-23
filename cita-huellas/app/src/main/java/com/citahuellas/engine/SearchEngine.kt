package com.citahuellas.engine

import com.citahuellas.data.AppSettings
import com.citahuellas.data.Profile
import com.citahuellas.data.clamped
import kotlin.random.Random

/** Qué hacer en la pantalla actual. Ninguna acción confirma la cita: eso lo hace siempre la persona. */
sealed interface Action {
    data object SelectProvince : Action
    data object SelectProcedure : Action
    data object EnterWithoutClave : Action
    data class FillPersonal(val profile: Profile) : Action
    data object Request : Action
    data class WaitAndRestart(val delayMs: Long, val attempt: Int) : Action
    /** Fallo de carga del navegador: esperar y volver a la portada, sin gastar intento. */
    data class RetryAfterError(val delayMs: Long, val retry: Int, val clearCookies: Boolean = false) : Action
    /** Página en blanco (comprobación de la web que se recarga sola): esperar y volver a mirar. */
    data class WaitForPage(val delayMs: Long) : Action
    data class FillContact(val phone: String, val email: String) : Action
    data class Found(val kind: PageKind) : Action
    data class Stop(val reason: String) : Action
}

class SearchEngine(
    private val profile: Profile,
    private val contactPhone: String,
    private val contactEmail: String,
    settings: AppSettings,
    private val random: Random = Random.Default,
) {
    private val settings = settings.clamped()

    var attempt: Int = 1
        private set
    var finished: Boolean = false
        private set
    /** Hay cita: desde aquí la app solo ayuda (rellena contacto una vez) y no pulsa nada más. */
    var found: Boolean = false
        private set

    private var stopReason = ""
    private var contactFilled = false
    private var lastKind: PageKind? = null
    private var sameKindCount = 0
    private var networkRetries = 0
    private var blankWaits = 0
    /** Ya se han enviado los datos: una pantalla rara a partir de aquí puede ser una cita. */
    private var sentData = false

    fun next(kind: PageKind): Action {
        if (finished) return Action.Stop(stopReason)
        if (found || kind == PageKind.OFFICE_AVAILABLE) return assist(kind)

        if (sentData && (kind == PageKind.UNKNOWN || kind == PageKind.CAPTCHA)) return assist(kind)

        if (kind == PageKind.NETWORK_ERROR) {
            lastKind = null
            sentData = false
            if (networkRetries >= 2) return stop("La web no responde tras varios reintentos. Espera un rato (o prueba con datos móviles).")
            networkRetries++
            return Action.RetryAfterError(random.nextLong(5_000, 10_001), networkRetries, clearCookies = networkRetries == 2)
        }
        networkRetries = 0

        if (kind == PageKind.BLANK) {
            lastKind = null
            if (blankWaits >= 4) return stop("La web se queda en blanco. Prueba \"Borrar cookies de la web\" o espera un rato.")
            blankWaits++
            return Action.WaitForPage(2_000)
        }
        blankWaits = 0

        sameKindCount = if (kind == lastKind) sameKindCount + 1 else 1
        lastKind = kind
        if (sameKindCount >= 4) return stop("La página no avanza")

        return when (kind) {
            PageKind.PROVINCE -> Action.SelectProvince
            PageKind.OFFICE_PROCEDURE -> Action.SelectProcedure
            PageKind.INSTRUCTIONS -> Action.EnterWithoutClave
            PageKind.PERSONAL_DATA -> { sentData = true; Action.FillPersonal(profile) }
            PageKind.REQUEST -> { sentData = true; Action.Request }
            PageKind.NO_APPOINTMENTS ->
                if (attempt < settings.maxAttempts) {
                    attempt++
                    lastKind = null
                    sentData = false
                    Action.WaitAndRestart(settings.intervalSec * 1000L, attempt)
                } else {
                    stop("Sin citas tras ${settings.maxAttempts} intentos")
                }
            PageKind.CONTACT, PageKind.SLOT_OR_SMS, PageKind.OFFICE_AVAILABLE -> assist(kind)
            PageKind.CAPTCHA -> stop("Hay un captcha: resuélvelo tú en la web")
            PageKind.NETWORK_ERROR, PageKind.BLANK -> error("tratado arriba")
            PageKind.THROTTLED -> stop("La web ha cortado la conexión: suele ser un bloqueo temporal por muchos intentos. Espera 30–60 minutos (o prueba con datos móviles).")
            PageKind.ERROR -> stop("La web ha dado un error o la sesión ha caducado")
            PageKind.UNKNOWN -> stop("Pantalla no reconocida: sigue tú a mano")
        }
    }

    private fun assist(kind: PageKind): Action {
        found = true
        if (kind == PageKind.CONTACT && !contactFilled) {
            contactFilled = true
            return Action.FillContact(contactPhone, contactEmail)
        }
        return Action.Found(kind)
    }

    private fun stop(reason: String): Action.Stop {
        finished = true
        stopReason = reason
        return Action.Stop(reason)
    }
}
