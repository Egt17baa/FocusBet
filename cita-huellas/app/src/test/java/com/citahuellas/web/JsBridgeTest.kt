package com.citahuellas.web

import com.citahuellas.data.Profile
import com.citahuellas.engine.Action
import com.citahuellas.engine.PageKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsBridgeTest {
    private val hostile = Profile("1", "O'Brien \"</script>\\", "X1234567L", "PERU", "6", "a@b.c")

    @Test fun fillEscapesHostileText() {
        val js = JsBridge.js(Action.FillPersonal(hostile))!!
        // El nombre viaja como cadena JSON escapada: comillas y barra invertida escapadas.
        assertTrue(js, js.contains("\"O'Brien \\\"</script>\\\\\""))
        assertTrue(js.startsWith("CH.run(") && js.endsWith(")"))
    }

    @Test fun waitAndStopProduceNoJs() {
        assertNull(JsBridge.js(Action.WaitAndRestart(1, 2)))
        assertNull(JsBridge.js(Action.Stop("x")))
        assertNull(JsBridge.js(Action.Found(PageKind.SLOT_OR_SMS)))
    }

    @Test fun fillContactNeverClicks() {
        val js = JsBridge.js(Action.FillContact("611", "yo@x.es"))!!
        assertFalse(js.contains("\"click\""))
        assertTrue(js.contains("emailUNO") && js.contains("emailDOS") && js.contains("txtTelefonoCitado"))
    }

    @Test fun provinceSelectsMadridThenAccepts() {
        assertEquals(
            "CH.run([[\"selectByText\",\"form\",\"MADRID\"],[\"click\",\"btnAceptar\"]])",
            JsBridge.js(Action.SelectProvince),
        )
    }

    @Test fun procedureUsesFingerprintTextAndKeepsAnyOffice() {
        val js = JsBridge.js(Action.SelectProcedure)!!
        assertTrue(js.contains("[\"selectByText\",\"tramiteGrupo[1]\",\"TOMA DE HUELLAS\"]"))
        assertFalse(js.contains("tramiteGrupo[0]"))
        assertFalse("no pulsa en el mismo paso: la web rellena el subtrámite", js.contains("\"click\""))
    }

    @Test fun procedureFollowUpChecksSubtramiteThenAccepts() {
        assertEquals(
            "CH.run([[\"needChoice\",\"subtramite\"],[\"click\",\"btnAceptar\"]])",
            JsBridge.followUp(Action.SelectProcedure),
        )
        assertNull(JsBridge.followUp(Action.SelectProvince))
    }

    @Test fun smsCodeIsFilledNotConfirmed() {
        val js = JsBridge.fillSmsCode("123456")
        assertTrue(js.contains("txtCodigoVerificacion"))
        assertFalse(js.contains("btnConfirmar"))
    }

    @Test fun enterWithoutClaveFallsBackToButtonText() {
        assertEquals(
            "CH.run([[\"clickAny\",\"btnEntrar\",\"SIN CL@VE\"]])",
            JsBridge.js(Action.EnterWithoutClave),
        )
    }

    @Test fun parseResult() {
        assertNull(JsBridge.missingField("\"ok\""))
        assertEquals("txtPaisNac", JsBridge.missingField("\"missing:txtPaisNac\""))
    }
}
