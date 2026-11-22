package com.citahuellas.engine

import com.citahuellas.data.AppSettings
import com.citahuellas.data.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEngineTest {
    private val ana = Profile("1", "Ana Pérez", "X1234567L", "PERU", "600000000", "ana@example.com")
    private fun engine(max: Int = 10) = SearchEngine(ana, "611111111", "yo@example.com", AppSettings(60, max))

    @Test fun happyPathOrder() {
        val e = engine()
        assertEquals(Action.SelectProvince, e.next(PageKind.PROVINCE))
        assertEquals(Action.SelectProcedure, e.next(PageKind.OFFICE_PROCEDURE))
        assertEquals(Action.EnterWithoutClave, e.next(PageKind.INSTRUCTIONS))
        assertEquals(Action.FillPersonal(ana), e.next(PageKind.PERSONAL_DATA))
        assertEquals(Action.Request, e.next(PageKind.REQUEST))
    }

    @Test fun retriesUntilMax() {
        val e = engine(10)
        val actions = (1..10).map {
            e.next(PageKind.PROVINCE)
            e.next(PageKind.NO_APPOINTMENTS)
        }
        actions.take(9).forEachIndexed { i, a -> assertEquals(Action.WaitAndRestart(60_000, i + 2), a) }
        assertEquals(Action.Stop("Sin citas tras 10 intentos"), actions[9])
        assertTrue(e.finished)
    }

    @Test fun officeAvailableIsFound() {
        val e = engine()
        assertEquals(Action.Found(PageKind.OFFICE_AVAILABLE), e.next(PageKind.OFFICE_AVAILABLE))
        assertFalse(e.finished)
        assertTrue(e.found)
    }

    @Test fun contactFilledOnceThenFound() {
        val e = engine()
        e.next(PageKind.OFFICE_AVAILABLE)
        assertEquals(Action.FillContact("611111111", "yo@example.com"), e.next(PageKind.CONTACT))
        assertEquals(Action.Found(PageKind.CONTACT), e.next(PageKind.CONTACT))
        assertEquals(Action.Found(PageKind.SLOT_OR_SMS), e.next(PageKind.SLOT_OR_SMS))
    }

    @Test fun afterFoundNothingIsAutomated() {
        val e = engine()
        e.next(PageKind.OFFICE_AVAILABLE)
        assertEquals(Action.Found(PageKind.PROVINCE), e.next(PageKind.PROVINCE))
        assertEquals(Action.Found(PageKind.NO_APPOINTMENTS), e.next(PageKind.NO_APPOINTMENTS))
    }

    @Test fun captchaStops() {
        val e = engine()
        assertEquals(Action.Stop("Hay un captcha: resuélvelo tú en la web"), e.next(PageKind.CAPTCHA))
        assertTrue(e.finished)
    }

    @Test fun unknownPageStops() {
        val e = engine()
        assertTrue(e.next(PageKind.UNKNOWN) is Action.Stop)
        assertTrue(e.finished)
    }

    @Test fun throttledAndErrorStop() {
        assertEquals(
            Action.Stop("La web ha cortado la conexión: suele ser un bloqueo temporal por muchos intentos. Espera 30–60 minutos (o prueba con datos móviles)."),
            engine().next(PageKind.THROTTLED),
        )
        assertTrue(engine().next(PageKind.ERROR) is Action.Stop)
    }

    @Test fun stuckPageStops() {
        val e = engine()
        repeat(3) { assertEquals(Action.FillPersonal(ana), e.next(PageKind.PERSONAL_DATA)) }
        assertEquals(Action.Stop("La página no avanza"), e.next(PageKind.PERSONAL_DATA))
    }

    @Test fun finishedEngineKeepsStopping() {
        val e = engine()
        e.next(PageKind.CAPTCHA)
        assertTrue(e.next(PageKind.PROVINCE) is Action.Stop)
    }

    @Test fun networkErrorRetriesTwiceThenStops() {
        val e = engine()
        val r1 = e.next(PageKind.NETWORK_ERROR)
        assertTrue(r1 is Action.RetryAfterError && r1.delayMs in 5_000..10_000)
        assertTrue(e.next(PageKind.NETWORK_ERROR) is Action.RetryAfterError)
        assertEquals(
            Action.Stop("La web no responde tras varios reintentos. Espera un rato (o prueba con datos móviles)."),
            e.next(PageKind.NETWORK_ERROR),
        )
        assertEquals("no gasta intentos", 1, e.attempt)
    }

    @Test fun networkRetryCounterResetsAfterProgress() {
        val e = engine()
        e.next(PageKind.NETWORK_ERROR); e.next(PageKind.NETWORK_ERROR)
        e.next(PageKind.PROVINCE)
        assertTrue(e.next(PageKind.NETWORK_ERROR) is Action.RetryAfterError)
        assertTrue(e.next(PageKind.NETWORK_ERROR) is Action.RetryAfterError)
    }

    @Test fun unknownPageAfterSendingDataIsPossibleCita() {
        val e = engine()
        e.next(PageKind.PERSONAL_DATA)
        assertEquals(Action.Found(PageKind.UNKNOWN), e.next(PageKind.UNKNOWN))
        assertTrue(e.found)
        assertFalse(e.finished)
    }

    @Test fun captchaAfterSendingDataAlertsInsteadOfStopping() {
        val e = engine()
        e.next(PageKind.REQUEST)
        assertEquals(Action.Found(PageKind.CAPTCHA), e.next(PageKind.CAPTCHA))
    }

    @Test fun unknownBeforeSendingDataStillStops() {
        val e = engine()
        e.next(PageKind.PROVINCE)
        assertTrue(e.next(PageKind.UNKNOWN) is Action.Stop)
    }

    @Test fun sentFlagResetsOnRestart() {
        val e = engine()
        e.next(PageKind.PERSONAL_DATA)
        assertTrue(e.next(PageKind.NO_APPOINTMENTS) is Action.WaitAndRestart)
        e.next(PageKind.PROVINCE)
        assertTrue(e.next(PageKind.UNKNOWN) is Action.Stop)
    }
}
