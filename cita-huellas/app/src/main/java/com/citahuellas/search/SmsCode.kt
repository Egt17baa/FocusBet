package com.citahuellas.search

private val CODE = Regex("(?<!\\d)(\\d{4,8})(?!\\d)")

/** Primer número de 4 a 8 cifras del SMS. */
fun extractSmsCode(message: String?): String? = message?.let { CODE.find(it)?.groupValues?.get(1) }
