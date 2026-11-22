package com.citahuellas.web

import com.citahuellas.engine.Action
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

/** Traduce cada [Action] a una llamada `CH.run([...])` de bridge.js. Los textos van siempre como JSON escapado. */
object JsBridge {
    private fun step(vararg parts: String) = JsonArray(parts.map(::JsonPrimitive))
    private fun run(vararg steps: JsonArray) = "CH.run(" + Json.encodeToString(JsonArray.serializer(), JsonArray(steps.toList())) + ")"

    fun js(action: Action): String? = when (action) {
        Action.SelectProvince -> run(step("selectByText", "form", "MADRID"), step("click", "btnAceptar"))
        Action.SelectProcedure -> run(
            step("trySelectByText", "sede", "CUALQUIER OFICINA"),
            step("selectByText", "tramiteGrupo[1]", "TOMA DE HUELLAS"),
            step("click", "btnAceptar"),
        )
        Action.EnterWithoutClave -> run(step("clickAny", "btnEntrar", "SIN CL@VE"))
        is Action.FillPersonal -> run(
            step("check", "rdbTipoDocNie"),
            step("setValue", "txtIdCitado", action.profile.nie.trim().uppercase()),
            step("setValue", "txtDesCitado", action.profile.fullName.trim()),
            step("selectByText", "txtPaisNac", action.profile.country),
            step("click", "btnEnviar"),
        )
        Action.Request -> run(step("click", "btnEnviar"))
        is Action.FillContact -> run(
            step("setValue", "txtTelefonoCitado", action.phone.filter(Char::isDigit)),
            step("setValue", "emailUNO", action.email.trim()),
            step("setValue", "emailDOS", action.email.trim()),
        )
        is Action.WaitAndRestart, is Action.Found, is Action.Stop -> null
    }

    fun fillSmsCode(code: String): String = run(step("setValue", "txtCodigoVerificacion", code))

    const val SNAPSHOT = "CH.snapshot()"
    const val CONTROLS = "CH.controls()"
    const val COUNTRIES = "CH.options('txtPaisNac')"

    /** `evaluateJavascript` devuelve el resultado como literal JSON. */
    fun missingField(result: String?): String? {
        val value = result?.let { runCatching { Json.decodeFromString<String>(it) }.getOrNull() } ?: return "?"
        return if (value == "ok") null else value.removePrefix("missing:")
    }

    /** Nombre legible del campo para el mensaje de parada. */
    fun fieldLabel(id: String): String = when (id) {
        "form" -> "la provincia"
        "tramiteGrupo[1]" -> "el trámite de toma de huellas"
        "txtPaisNac" -> "el país de nacionalidad"
        "txtIdCitado" -> "el campo NIE"
        "txtDesCitado" -> "el campo nombre"
        "btnEntrar" -> "el botón Presentación sin Cl@ve"
        else -> id
    }
}
