package com.citahuellas.search

/** Lo que el WebView ha visto en la última carga, para explicar por qué se ha parado. */
class PageDiagnostics {
    private var url: String? = null
    private val problems = mutableListOf<String>()
    private var text: String? = null

    @Synchronized fun onLoad(url: String) {
        this.url = url
        problems.clear()
        text = null
    }
    @Synchronized fun onError(code: Int, description: String) { problems += "error $code $description" }
    @Synchronized fun onHttpError(status: Int) { problems += "HTTP $status" }
    @Synchronized fun onBlocked(url: String) { problems += "redirección bloqueada a $url" }
    @Synchronized fun onText(t: String?) { text = t }

    @Synchronized fun summary(): String = buildList {
        url?.let { add("URL: $it") }
        addAll(problems)
        text?.let { t ->
            val clean = t.replace(Regex("\\s+"), " ").trim()
            add("texto: " + if (clean.isEmpty()) "(vacío)" else clean.take(120))
        }
    }.joinToString(" · ")
}
