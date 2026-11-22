package com.citahuellas.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsCodeTest {
    @Test fun extractsSixDigitCode() =
        assertEquals("482913", extractSmsCode("Su codigo de verificacion para la cita previa es 482913"))
    @Test fun ignoresShortNumbers() =
        assertEquals("12345", extractSmsCode("Paso 2 de 3: codigo 12345"))
    @Test fun noCode() = assertNull(extractSmsCode("Hola, sin codigo"))
    @Test fun nullMessage() = assertNull(extractSmsCode(null))
}
