package com.citahuellas.web

import com.citahuellas.engine.PageSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnapshotParserTest {
    @Test fun parsesDoubleEncodedResult() {
        // evaluateJavascript devuelve el string JSON de CH.snapshot() como literal JSON.
        val raw = "\"{\\\"url\\\":\\\"https://x/\\\",\\\"ids\\\":[\\\"form\\\",\\\"btnAceptar\\\"],\\\"text\\\":\\\"HOLA\\\"}\""
        assertEquals(PageSnapshot("https://x/", setOf("form", "btnAceptar"), "HOLA"), parseSnapshot(raw))
    }
    @Test fun nullOrGarbage() {
        assertNull(parseSnapshot(null))
        assertNull(parseSnapshot("null"))
        assertNull(parseSnapshot("\"no es json\""))
    }
}
