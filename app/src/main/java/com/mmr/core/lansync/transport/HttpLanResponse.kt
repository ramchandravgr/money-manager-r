package com.mmr.core.lansync.transport

data class HttpLanResponse(
    val status: Int,
    val body: ByteArray = ByteArray(0),
    val contentType: String = "application/json; charset=utf-8",
    val extraHeaders: Map<String, String> = emptyMap(),
) {
    companion object {
        fun json(
            status: Int,
            text: String,
        ): HttpLanResponse = HttpLanResponse(status, text.toByteArray(Charsets.UTF_8))

        fun html(
            status: Int,
            text: String,
        ): HttpLanResponse = HttpLanResponse(status, text.toByteArray(Charsets.UTF_8), "text/html; charset=utf-8")

        fun bytes(
            status: Int,
            payload: ByteArray,
            contentType: String,
        ): HttpLanResponse = HttpLanResponse(status, payload, contentType)
    }
}
