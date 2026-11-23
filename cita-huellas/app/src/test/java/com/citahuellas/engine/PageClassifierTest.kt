package com.citahuellas.engine

import org.junit.Assert.assertEquals
import org.junit.Test

private const val FILLER = " INTERNET CITA PREVIA - TEXTO DE RELLENO SUFICIENTE "

fun snap(vararg ids: String, text: String = "") = PageSnapshot("https://x/", ids.toSet(), FILLER + text)

class PageClassifierTest {
    private fun k(s: PageSnapshot) = PageClassifier.classify(s)

    @Test fun province() = assertEquals(PageKind.PROVINCE, k(snap("form", "btnAceptar")))
    @Test fun officeProcedure() = assertEquals(PageKind.OFFICE_PROCEDURE, k(snap("form", "sede", "tramiteGrupo[1]", "btnAceptar")))
    @Test fun instructions() = assertEquals(PageKind.INSTRUCTIONS, k(snap("btnEntrar", text = "PRESENTACIÓN SIN CL@VE")))
    @Test fun instructionsByTextWhenButtonRenamed() =
        assertEquals(PageKind.INSTRUCTIONS, k(snap("sede", "tramiteGrupo[1]", "btnSinClave", text = "Presentación sin Cl@ve")))
    @Test fun personalData() = assertEquals(PageKind.PERSONAL_DATA, k(snap("txtIdCitado", "txtDesCitado", "btnEnviar")))
    @Test fun request() = assertEquals(PageKind.REQUEST, k(snap("btnEnviar", "btnConsultar", text = "SOLICITAR CITA")))
    @Test fun noAppointments() = assertEquals(PageKind.NO_APPOINTMENTS, k(snap("btnSalir", text = "En este momento no hay citas disponibles.")))
    @Test fun officeAvailable() = assertEquals(PageKind.OFFICE_AVAILABLE, k(snap("idSede", "btnSiguiente")))
    @Test fun contact() = assertEquals(PageKind.CONTACT, k(snap("txtTelefonoCitado", "emailUNO", "emailDOS", "btnSiguiente")))
    @Test fun slotOrSms() = assertEquals(PageKind.SLOT_OR_SMS, k(snap("txtCodigoVerificacion", "btnConfirmar")))
    @Test fun captchaWinsOverPersonalData() = assertEquals(PageKind.CAPTCHA, k(snap("txtIdCitado", "reCAPTCHA_site_key")))
    @Test fun captchaImage() = assertEquals(PageKind.CAPTCHA, k(snap("__captchaimg", "btnEnviar")))
    @Test fun unknownPage() = assertEquals(PageKind.UNKNOWN, k(snap("algoNuevo")))
    @Test fun blankOrThrottleStops() {
        assertEquals(PageKind.BLANK, k(PageSnapshot("https://x/", emptySet(), "")))
        assertEquals(PageKind.THROTTLED, k(snap(text = "Too Many Requests")))
        assertEquals(PageKind.THROTTLED, k(snap(text = "The requested URL was rejected. Please consult with your administrator.")))
    }
    @Test fun browserErrorPageIsNetworkError() = assertEquals(
        PageKind.NETWORK_ERROR,
        k(snap(text = "Página web no disponible No ha sido posible cargar la página web https://icp... net::ERR_EMPTY_RESPONSE")),
    )
    @Test fun citaFieldsWinOverNoAppointmentsText() {
        assertEquals(PageKind.OFFICE_AVAILABLE, k(snap("idSede", "btnSiguiente", text = "En otras oficinas no hay citas")))
        assertEquals(PageKind.SLOT_OR_SMS, k(snap("rdbCita", text = "No hay citas por la tarde")))
    }
    @Test fun sessionExpiredIsError() = assertEquals(PageKind.ERROR, k(snap("btnAceptar", text = "Su sesión ha caducado")))
}
