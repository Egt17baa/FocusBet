package com.citahuellas.engine

import com.citahuellas.data.AppSettings
import com.citahuellas.data.Profile
import com.citahuellas.data.clamped

/** Qué hacer en la pantalla actual. Ninguna acción confirma la cita: eso lo hace siempre la persona. */
sealed interface Action {
    data object SelectProvince : Action
    data object SelectProcedure : Action
    data object EnterWithoutClave : Action
    data class FillPersonal(val profile: Profile) : Action
    data object Request : Action
    data class WaitAndRestart(val delayMs: Long, val attempt: Int) : Action
    data class FillContact(val phone: String, val email: String) : Action
    data class Found(val kind: PageKind) : Action
    data class Stop(val reason: String) : Action
}

class SearchEngine(
    private val profile: Profile,
    private val contactPhone: String,
    private val contactEmail: String,
    settings: AppSettings,
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

    fun next(kind: PageKind): Action {
        if (finished) return Action.Stop(stopReason)
        if (found || kind == PageKind.OFFICE_AVAILABLE) return assist(kind)

        sameKindCount = if (kind == lastKind) sameKindCount + 1 else 1
        lastKind = kind
        if (sameKindCount >= 4) return stop("La página no avanza")

        return when (kind) {
            PageKind.PROVINCE -> Action.SelectProvince
            PageKind.OFFICE_PROCEDURE -> Action.SelectProcedure
            PageKind.INSTRUCTIONS -> Action.EnterWithoutClave
            PageKind.PERSONAL_DATA -> Action.FillPersonal(profile)
            PageKind.REQUEST -> Action.Request
            PageKind.NO_APPOINTMENTS ->
                if (attempt < settings.maxAttempts) {
                    attempt++
                    lastKind = null
                    Action.WaitAndRestart(settings.intervalSec * 1000L, attempt)
                } else {
                    stop("Sin citas tras ${settings.maxAttempts} intentos")
                }
            PageKind.CONTACT, PageKind.SLOT_OR_SMS, PageKind.OFFICE_AVAILABLE -> assist(kind)
            PageKind.CAPTCHA -> stop("Hay un captcha: resuélvelo tú en la web")
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
