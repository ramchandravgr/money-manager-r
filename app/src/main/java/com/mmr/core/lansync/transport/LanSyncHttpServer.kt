package com.mmr.core.lansync.transport

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class LanSyncHttpServer(
    private val scope: CoroutineScope,
    private val requestHandler: (path: String, headers: Map<String, String>, body: String) -> Pair<Int, String>,
) {
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private val running = AtomicBoolean(false)

    val port: Int
        get() = serverSocket?.localPort ?: LanSyncHttpConstants.DEFAULT_PORT

    fun start(preferredPort: Int = LanSyncHttpConstants.DEFAULT_PORT) {
        if (running.getAndSet(true)) return
        serverSocket = ServerSocket(preferredPort)
        acceptJob =
            scope.launch(Dispatchers.IO) {
                val socket = serverSocket ?: return@launch
                while (isActive && running.get()) {
                    runCatching {
                        handleClient(socket.accept())
                    }
                }
            }
    }

    fun stop() {
        if (!running.getAndSet(false)) return
        acceptJob?.cancel()
        acceptJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    private fun handleClient(client: Socket) {
        client.soTimeout = 30_000
        client.use { socket ->
            val input = BufferedInputStream(socket.getInputStream())
            val request = readHttpRequest(input) ?: return
            val (status, responseBody) =
                requestHandler(request.path, request.headers, request.body)
            val responseBytes = buildResponse(status, responseBody)
            BufferedOutputStream(socket.getOutputStream()).use { out ->
                out.write(responseBytes)
                out.flush()
            }
        }
    }

    private data class HttpRequest(
        val path: String,
        val headers: Map<String, String>,
        val body: String,
    )

    private fun readHttpRequest(input: BufferedInputStream): HttpRequest? {
        val headerBytes = readUntilHeaderEnd(input) ?: return null
        val headerText = headerBytes.toString(Charsets.UTF_8)
        val lines = headerText.split("\r\n")
        if (lines.isEmpty()) return null
        val requestLine = lines.first().split(" ")
        if (requestLine.size < 2) return null
        val path = requestLine[1].substringBefore('?')
        val headers = mutableMapOf<String, String>()
        for (index in 1 until lines.size) {
            val line = lines[index]
            if (line.isBlank()) continue
            val colon = line.indexOf(':')
            if (colon <= 0) continue
            val key = line.substring(0, colon).trim().lowercase()
            val value = line.substring(colon + 1).trim()
            headers[key] = value
        }
        val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
        val body =
            if (contentLength > 0) {
                val bodyBytes = ByteArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val chunk = input.read(bodyBytes, read, contentLength - read)
                    if (chunk < 0) break
                    read += chunk
                }
                bodyBytes.toString(Charsets.UTF_8)
            } else {
                ""
            }
        return HttpRequest(path, headers, body)
    }

    private fun readUntilHeaderEnd(input: BufferedInputStream): ByteArray? {
        val buffer = ArrayList<Byte>()
        var state = 0
        while (true) {
            val byte = input.read()
            if (byte < 0) return if (buffer.isEmpty()) null else buffer.toByteArray()
            buffer.add(byte.toByte())
            state =
                when (state) {
                    0 -> if (byte == '\r'.code) 1 else 0
                    1 -> if (byte == '\n'.code) 2 else 0
                    2 -> if (byte == '\r'.code) 3 else 0
                    3 -> if (byte == '\n'.code) return buffer.toByteArray() else 0
                    else -> 0
                }
        }
    }

    private fun buildResponse(
        status: Int,
        body: String,
    ): ByteArray {
        val statusText =
            when (status) {
                200 -> "OK"
                401 -> "Unauthorized"
                403 -> "Forbidden"
                404 -> "Not Found"
                405 -> "Method Not Allowed"
                else -> "Error"
            }
        val payload = body.toByteArray(Charsets.UTF_8)
        val header =
            "HTTP/1.1 $status $statusText\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${payload.size}\r\n" +
                "Connection: close\r\n\r\n"
        return header.toByteArray(Charsets.UTF_8) + payload
    }
}
