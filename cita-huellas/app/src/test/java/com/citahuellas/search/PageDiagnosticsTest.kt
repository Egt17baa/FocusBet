package com.citahuellas.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageDiagnosticsTest {
    @Test fun summaryListsEverythingSeen() {
        val d = PageDiagnostics()
        d.onLoad("https://icp.administracionelectronica.gob.es/icpplustiem/index.html")
        d.onError(-6, "net::ERR_CONNECTION_RESET")
        d.onBlocked("https://otra.example.com/x")
        d.onText("   ")
        val s = d.summary()
        assertTrue(s, s.contains("icpplustiem/index.html"))
        assertTrue(s, s.contains("ERR_CONNECTION_RESET"))
        assertTrue(s, s.contains("otra.example.com"))
        assertTrue(s, s.contains("texto: (vacío)"))
    }

    @Test fun httpErrorAndShortText() {
        val d = PageDiagnostics()
        d.onHttpError(403)
        d.onText("Request Rejected  " + "x".repeat(200))
        val s = d.summary()
        assertTrue(s, s.contains("HTTP 403"))
        assertTrue(s, s.contains("texto: Request Rejected"))
        assertTrue("texto recortado", s.length < 400)
    }

    @Test fun resetClearsPreviousPage() {
        val d = PageDiagnostics()
        d.onHttpError(500)
        d.onLoad("https://a/")
        assertEquals("URL: https://a/", d.summary())
    }

    @Test fun eventLogSurvivesNewLoadsAndIsCapped() {
        val d = PageDiagnostics()
        repeat(30) { d.event("finished #$it") }
        d.onLoad("https://b/")
        val full = d.fullReport()
        assertTrue(full, full.contains("finished #29"))
        assertTrue(full, !full.contains("finished #5\n"))
        assertTrue(full, full.contains("inicio https://b/"))
    }

    @Test fun pageDebugIsInFullReport() {
        val d = PageDiagnostics()
        d.onPageDebug("{\"href\":\"https://x/\",\"htmlLen\":1234}")
        assertTrue(d.fullReport().contains("htmlLen"))
        assertTrue(d.summary().contains("htmlLen"))
    }

    @Test fun sslErrorIsExplained() {
        val d = PageDiagnostics()
        d.onSslError(3, "https://icp.x/", "CN=*.administracionelectronica.gob.es", "CN=AC Componentes Informaticos")
        val s = d.summary()
        assertTrue(s, s.contains("certificado"))
        assertTrue(s, s.contains("no es de confianza"))
        assertTrue(s, s.contains("AC Componentes Informaticos"))
    }
}
