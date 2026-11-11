package com.rrparallelstudio.miapp.core.lansync.transport

import android.net.Network
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.InetSocketAddress
import java.net.Socket

object LanSyncHttpClient {
    fun post(
        host: String,
        port: Int,
        path: String,
        deviceId: String,
        authToken: String,
        body: String,
        timeoutMs: Int = 30_000,
        network: Network? = null,
    ): HttpResponse {
        connectedSocket(host, port, timeoutMs, network).use { socket ->
            val request =
                buildString {
                    append("POST $path HTTP/1.1\r\n")
                    append("Host: $host:$port\r\n")
                    append("${LanSyncHttpConstants.HEADER_DEVICE_ID}: $deviceId\r\n")
                    append("${LanSyncHttpConstants.HEADER_AUTH_TOKEN}: $authToken\r\n")
                    append("Content-Type: application/json; charset=utf-8\r\n")
                    append("Content-Length: ${body.toByteArray(Charsets.UTF_8).size}\r\n")
                    append("Connection: close\r\n\r\n")
                    append(body)
                }
            writeRequest(socket, request)
            return readResponse(BufferedInputStream(socket.getInputStream()))
        }
    }

    fun get(
        host: String,
        port: Int,
        path: String,
        timeoutMs: Int = 15_000,
        network: Network? = null,
    ): HttpResponse {
        connectedSocket(host, port, timeoutMs, network).use { socket ->
            val request =
                buildString {
                    append("GET $path HTTP/1.1\r\n")
                    append("Host: $host:$port\r\n")
                    append("Connection: close\r\n\r\n")
                }
            writeRequest(socket, request)
            return readResponse(BufferedInputStream(socket.getInputStream()))
        }
    }

    fun getBytes(
        host: String,
        port: Int,
        path: String,
        deviceId: String,
        authToken: String,
        timeoutMs: Int = 60_000,
        network: Network? = null,
    ): BinaryHttpResponse {
        connectedSocket(host, port, timeoutMs, network).use { socket ->
            val request =
                buildString {
                    append("GET $path HTTP/1.1\r\n")
                    append("Host: $host:$port\r\n")
                    append("${LanSyncHttpConstants.HEADER_DEVICE_ID}: $deviceId\r\n")
                    append("${LanSyncHttpConstants.HEADER_AUTH_TOKEN}: $authToken\r\n")
                    append("Connection: close\r\n\r\n")
                }
            writeRequest(socket, request)
            return readBinaryResponse(BufferedInputStream(socket.getInputStream()))
        }
    }

    /**
     * Flush only. Closing [Socket.getOutputStream] closes the socket, so the reply cannot be read.
     */
    private fun writeRequest(
        socket: Socket,
        request: String,
    ) {
        val out = BufferedOutputStream(socket.getOutputStream())
        out.write(request.toByteArray(Charsets.UTF_8))
        out.flush()
    }

    private fun connectedSocket(
        host: String,
        port: Int,
        timeoutMs: Int,
        network: Network?,
    ): Socket {
        if (network == null) {
            return connectSocket(host, port, timeoutMs, bindNetwork = null)
        }
        return try {
            connectSocket(host, port, timeoutMs, bindNetwork = network)
        } catch (_: Exception) {
            connectSocket(host, port, timeoutMs, bindNetwork = null)
        }
    }

    private fun connectSocket(
        host: String,
        port: Int,
        timeoutMs: Int,
        bindNetwork: Network?,
    ): Socket {
        val socket = Socket()
        try {
            bindNetwork?.bindSocket(socket)
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            socket.soTimeout = timeoutMs
            return socket
        } catch (error: Exception) {
            runCatching { socket.close() }
            throw error
        }
    }

    data class HttpResponse(
        val statusCode: Int,
        val body: String,
    )

    data class BinaryHttpResponse(
        val statusCode: Int,
        val body: ByteArray,
        val contentType: String?,
    )

    private fun readResponse(input: BufferedInputStream): HttpResponse {
        val headerBytes = readUntilHeaderEnd(input) ?: return HttpResponse(0, "")
        val headerText = headerBytes.toString(Charsets.UTF_8)
        val statusLine = headerText.lineSequence().firstOrNull() ?: return HttpResponse(0, "")
        val statusCode = statusLine.split(" ").getOrNull(1)?.toIntOrNull() ?: 0
        val contentLength =
            headerText
                .lineSequence()
                .drop(1)
                .mapNotNull { line ->
                    val colon = line.indexOf(':')
                    if (colon <= 0) return@mapNotNull null
                    val key = line.substring(0, colon).trim().lowercase()
                    val value = line.substring(colon + 1).trim()
                    if (key == "content-length") value.toIntOrNull() else null
                }.firstOrNull() ?: 0
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
        return HttpResponse(statusCode, body)
    }

    private fun readBinaryResponse(input: BufferedInputStream): BinaryHttpResponse {
        val headerBytes = readUntilHeaderEnd(input) ?: return BinaryHttpResponse(0, ByteArray(0), null)
        val headerText = headerBytes.toString(Charsets.UTF_8)
        val statusLine = headerText.lineSequence().firstOrNull() ?: return BinaryHttpResponse(0, ByteArray(0), null)
        val statusCode = statusLine.split(" ").getOrNull(1)?.toIntOrNull() ?: 0
        var contentType: String? = null
        val contentLength =
            headerText
                .lineSequence()
                .drop(1)
                .mapNotNull { line ->
                    val colon = line.indexOf(':')
                    if (colon <= 0) return@mapNotNull null
                    val key = line.substring(0, colon).trim().lowercase()
                    val value = line.substring(colon + 1).trim()
                    when (key) {
                        "content-length" -> value.toIntOrNull()
                        "content-type" -> {
                            contentType = value
                            null
                        }
                        else -> null
                    }
                }.firstOrNull() ?: 0
        val body =
            if (contentLength > 0) {
                val bodyBytes = ByteArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val chunk = input.read(bodyBytes, read, contentLength - read)
                    if (chunk < 0) break
                    read += chunk
                }
                bodyBytes
            } else {
                ByteArray(0)
            }
        return BinaryHttpResponse(statusCode, body, contentType)
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
}
