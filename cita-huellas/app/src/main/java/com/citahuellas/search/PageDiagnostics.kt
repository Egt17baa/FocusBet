package com.citahuellas.search

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Lo que el WebView ha visto, para explicar por qué se ha parado. */
class PageDiagnostics {
    private var url: String? = null
    private val problems = mutableListOf<String>()
    private var text: String? = null
    private var pageDebug: String? = null
    private var controls: String? = null
    private var stop: String? = null
    private val log = ArrayDeque<String>()
    private val clock = SimpleDateFormat("HH:mm:ss", Locale.ROOT)

    @Synchronized fun event(what: String) {
        log.addLast(clock.format(Date()) + " " + what)
        while (log.size > 20) log.removeFirst()
    }

    @Synchronized fun onLoad(url: String) {
        this.url = url
        problems.clear()
        text = null
        pageDebug = null
        event("inicio $url")
    }
    @Synchronized fun onError(code: Int, description: String) { problems += "error $code $description"; event("error $code $description") }
    @Synchronized fun onHttpError(status: Int) { problems += "HTTP $status"; event("HTTP $status") }
    @Synchronized fun onBlocked(url: String) { problems += "redirección bloqueada a $url"; event("bloqueada $url") }
    @Synchronized fun onSslError(primary: Int, url: String, subject: String, issuer: String) {
        val why = when (primary) {
            0 -> "aún no es válido"; 1 -> "ha caducado"; 2 -> "no corresponde al dominio"
            3 -> "no es de confianza para Android"; 4 -> "tiene fecha no válida"; else -> "no es válido"
        }
        val entry = "certificado SSL $why (emisor: $issuer; titular: $subject)"
        if (entry !in problems) problems += entry
        event("SSL $primary $url emisor=$issuer")
    }
    @Synchronized fun hasSslError(): Boolean = problems.any { it.startsWith("certificado SSL") }
    @Synchronized fun onText(t: String?) { text = t }
    @Synchronized fun onPageDebug(json: String?) { pageDebug = json }
    @Synchronized fun onControls(c: String?) { controls = c }
    @Synchronized fun onStop(reason: String, kind: String) { stop = "$reason [pantalla: $kind]"; event("parada: $reason") }

    @Synchronized fun summary(): String = buildList {
        url?.let { add("URL: $it") }
        addAll(problems)
        text?.let { t ->
            val clean = t.replace(Regex("\\s+"), " ").trim()
            add("texto: " + if (clean.isEmpty()) "(vacío)" else clean.take(120))
        }
        pageDebug?.let { add("página: " + it.take(160)) }
    }.joinToString(" · ")

    /** Informe completo para copiar y pegar. */
    @Synchronized fun fullReport(): String = buildString {
        appendLine("== Cita Huellas: diagnóstico ==")
        stop?.let { appendLine("motivo: $it") }
        appendLine(summary())
        pageDebug?.let { appendLine("página completa: $it") }
        controls?.let { appendLine("campos: $it") }
        appendLine("eventos:")
        log.forEach { appendLine("  $it") }
    }
}
