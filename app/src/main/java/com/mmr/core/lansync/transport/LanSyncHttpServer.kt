package com.mmr.core.lansync.transport

import com.mmr.core.lansync.LanSyncPortBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class LanSyncHttpServer(
    private val scope: CoroutineScope,
    private val requestHandler: (
        method: String,
        path: String,
        query: String,
        headers: Map<String, String>,
        body: String,
    ) -> HttpLanResponse,
    private val maxConcurrentConnections: Int = 8,
) {
    private val activeConnections =
        java.util.concurrent.atomic
            .AtomicInteger(0)
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private val running = AtomicBoolean(false)

    val port: Int
        get() = serverSocket?.localPort ?: LanSyncHttpConstants.DEFAULT_PORT

    /**
     * @param preferredPort When null (default), binds to port **0** so the OS assigns an ephemeral port.
     *   Pass a fixed port for tests or debugging.
     */
    fun start(preferredPort: Int? = null) {
        if (running.getAndSet(true)) return
        serverSocket =
            ServerSocket().apply {
                reuseAddress = true
                val bindPort =
                    if (preferredPort != null) {
                        LanSyncPortBinder.findAvailablePort(preferredPort)
                    } else {
                        LanSyncPortBinder.EPHEMERAL_BIND_PORT
                    }
                bind(InetSocketAddress("0.0.0.0", bindPort))
            }
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
        if (activeConnections.incrementAndGet() > maxConcurrentConnections) {
            activeConnections.decrementAndGet()
            client.use { socket ->
                val busy = buildResponse(HttpLanResponse.json(503, """{"error":"server_busy"}"""))
                BufferedOutputStream(socket.getOutputStream()).use { out ->
                    out.write(busy)
                    out.flush()
                }
            }
            return
        }
        try {
            client.soTimeout = 30_000
            client.use { socket ->
                val input = BufferedInputStream(socket.getInputStream())
                val request = readHttpRequest(input) ?: return
                val response =
                    requestHandler(
                        request.method,
                        request.path,
                        request.query,
                        request.headers,
                        request.body,
                    )
                val responseBytes = buildResponse(response)
                BufferedOutputStream(socket.getOutputStream()).use { out ->
                    out.write(responseBytes)
                    out.flush()
                }
            }
        } finally {
            activeConnections.decrementAndGet()
        }
    }

    private data class HttpRequest(
        val method: String,
        val path: String,
        val query: String,
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
        val method = requestLine[0].uppercase()
        val rawPath = requestLine[1]
        val path = rawPath.substringBefore('?')
        val query = rawPath.substringAfter('?', "")
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
        val body = readRequestBody(input, method, headers)
        return HttpRequest(method, path, query, headers, body)
    }

    private fun readRequestBody(
        input: BufferedInputStream,
        method: String,
        headers: Map<String, String>,
    ): String {
        val contentLength = headers["content-length"]?.toIntOrNull()
        if (contentLength != null && contentLength > 0) {
            val bodyBytes = ByteArray(contentLength)
            var read = 0
            while (read < contentLength) {
                val chunk = input.read(bodyBytes, read, contentLength - read)
                if (chunk < 0) break
                read += chunk
            }
            return bodyBytes.toString(Charsets.UTF_8)
        }
        if (headers["transfer-encoding"]?.contains("chunked", ignoreCase = true) == true) {
            return readChunkedBody(input)
        }
        if (method == "POST" || method == "PUT" || method == "PATCH") {
            return readUntilEof(input, MAX_BODY_BYTES)
        }
        return ""
    }

    private fun readUntilEof(
        input: BufferedInputStream,
        maxBytes: Int,
    ): String {
        val buffer = ByteArray(maxBytes)
        var total = 0
        while (total < maxBytes) {
            val read = input.read(buffer, total, maxBytes - total)
            if (read < 0) break
            total += read
        }
        return if (total == 0) "" else buffer.copyOf(total).toString(Charsets.UTF_8)
    }

    private fun readChunkedBody(input: BufferedInputStream): String {
        val output = StringBuilder()
        while (true) {
            val sizeLine = readLine(input) ?: break
            val chunkSize = sizeLine.substringBefore(';').trim().toIntOrNull(16) ?: break
            if (chunkSize == 0) {
                readLine(input)
                break
            }
            val chunk = ByteArray(chunkSize)
            var read = 0
            while (read < chunkSize) {
                val n = input.read(chunk, read, chunkSize - read)
                if (n < 0) break
                read += n
            }
            output.append(chunk.toString(Charsets.UTF_8))
            readLine(input)
        }
        return output.toString()
    }

    private fun readLine(input: BufferedInputStream): String? {
        val bytes = ArrayList<Byte>()
        while (true) {
            val b = input.read()
            if (b < 0) return if (bytes.isEmpty()) null else bytes.toByteArray().toString(Charsets.UTF_8)
            if (b == '\n'.code) {
                if (bytes.isNotEmpty() && bytes.last() == '\r'.code.toByte()) {
                    bytes.removeAt(bytes.size - 1)
                }
                return bytes.toByteArray().toString(Charsets.UTF_8)
            }
            bytes.add(b.toByte())
        }
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

    private fun buildResponse(response: HttpLanResponse): ByteArray {
        val status = response.status
        val statusText =
            when (status) {
                200 -> "OK"
                204 -> "No Content"
                400 -> "Bad Request"
                401 -> "Unauthorized"
                403 -> "Forbidden"
                404 -> "Not Found"
                405 -> "Method Not Allowed"
                409 -> "Conflict"
                503 -> "Service Unavailable"
                else -> "Error"
            }
        val payload = response.body
        val extra =
            response.extraHeaders.entries.joinToString("") { (key, value) ->
                "$key: $value\r\n"
            }
        val header =
            "HTTP/1.1 $status $statusText\r\n" +
                "Content-Type: ${response.contentType}\r\n" +
                extra +
                "Content-Length: ${payload.size}\r\n" +
                "Connection: close\r\n\r\n"
        return header.toByteArray(Charsets.UTF_8) + payload
    }

    private companion object {
        const val MAX_BODY_BYTES = 1_048_576
    }
}
