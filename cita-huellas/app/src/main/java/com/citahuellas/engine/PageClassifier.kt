package com.citahuellas.engine

import java.text.Normalizer

/** Lo que el JS ve de la página: ids presentes y texto visible. */
data class PageSnapshot(val url: String, val ids: Set<String>, val text: String)

enum class PageKind {
    PROVINCE, OFFICE_PROCEDURE, INSTRUCTIONS, PERSONAL_DATA, REQUEST, NO_APPOINTMENTS,
    OFFICE_AVAILABLE, CONTACT, SLOT_OR_SMS, CAPTCHA, NETWORK_ERROR, THROTTLED, ERROR, UNKNOWN,
}

/** Mayúsculas y sin tildes, para comparar textos de la web. */
fun normalizeText(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").uppercase()

object PageClassifier {
    /** Id sintético que el JS añade si ve una imagen de captcha (`img.img-thumbnail`). */
    const val CAPTCHA_IMAGE_ID = "__captchaimg"

    private val throttleTexts = listOf(
        "TOO MANY", "DEMASIADAS", "REQUESTED URL WAS REJECTED", "REQUEST REJECTED", "ACCESS DENIED",
    )
    /** Página de error del propio navegador (conexión cortada, sin respuesta…): se arregla recargando. */
    private val browserErrorTexts = listOf(
        "PAGINA WEB NO DISPONIBLE", "NO HA SIDO POSIBLE CARGAR LA PAGINA", "WEBPAGE NOT AVAILABLE", "NET::ERR_",
    )
    private val errorTexts = listOf("SESION HA CADUCADO", "SE HA PRODUCIDO UN ERROR", "ERROR 404", "ERROR 500", "SERVICE UNAVAILABLE")
    private val noAppointmentTexts = listOf("NO HAY CITAS", "EN ESTE MOMENTO NO HAY")

    fun classify(s: PageSnapshot): PageKind {
        val text = normalizeText(s.text)
        val ids = s.ids
        fun has(vararg wanted: String) = wanted.any { it in ids }
        fun says(list: List<String>) = list.any { it in text }

        return when {
            has("reCAPTCHA_site_key", "captcha", CAPTCHA_IMAGE_ID) -> PageKind.CAPTCHA
            says(browserErrorTexts) -> PageKind.NETWORK_ERROR
            text.trim().length < 20 || says(throttleTexts) -> PageKind.THROTTLED
            says(errorTexts) -> PageKind.ERROR
            says(noAppointmentTexts) -> PageKind.NO_APPOINTMENTS
            has("txtCodigoVerificacion", "rdbCita", "CitaMAP_HORAS", "btnConfirmar") -> PageKind.SLOT_OR_SMS
            has("txtTelefonoCitado") -> PageKind.CONTACT
            has("idSede") -> PageKind.OFFICE_AVAILABLE
            has("txtIdCitado") -> PageKind.PERSONAL_DATA
            has("btnEnviar") && (has("btnConsultar") || "SOLICITAR CITA" in text) -> PageKind.REQUEST
            has("btnEntrar") || "SIN CL@VE" in text -> PageKind.INSTRUCTIONS
            has("tramiteGrupo[1]", "sede") -> PageKind.OFFICE_PROCEDURE
            has("form") && has("btnAceptar") -> PageKind.PROVINCE
            else -> PageKind.UNKNOWN
        }
    }
}
