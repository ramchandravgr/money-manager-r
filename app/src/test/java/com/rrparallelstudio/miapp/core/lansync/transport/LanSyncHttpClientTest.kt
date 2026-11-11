package com.rrparallelstudio.miapp.core.lansync.transport

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.BufferedInputStream
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class LanSyncHttpClientTest {
    @Test
    fun get_readsBodyAfterSendingRequest() {
        ServerSocket(0).use { server ->
            val failure = AtomicReference<Throwable?>()
            val serverThread =
                thread {
                    try {
                        server.accept().use { client ->
                            discardRequest(BufferedInputStream(client.getInputStream()))
                            val body = """{"deviceId":"peer"}"""
                            val response =
                                "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: application/json\r\n" +
                                    "Content-Length: ${body.toByteArray(Charsets.UTF_8).size}\r\n" +
                                    "Connection: close\r\n\r\n" +
                                    body
                            val out = client.getOutputStream()
                            out.write(response.toByteArray(Charsets.UTF_8))
                            out.flush()
                        }
                    } catch (error: Throwable) {
                        failure.set(error)
                    }
                }
            val response =
                LanSyncHttpClient.get(
                    host = "127.0.0.1",
                    port = server.localPort,
                    path = "/mmr/v1/pairing-offer?code=ABC",
                    timeoutMs = 5_000,
                )
            serverThread.join(5_000)
            failure.get()?.let { throw it }
            assertEquals(200, response.statusCode)
            assertEquals("""{"deviceId":"peer"}""", response.body)
        }
    }

    private fun discardRequest(input: BufferedInputStream) {
        var state = 0
        while (true) {
            val byte = input.read()
            if (byte < 0) return
            state =
                when (state) {
                    0 -> if (byte == '\r'.code) 1 else 0
                    1 -> if (byte == '\n'.code) 2 else 0
                    2 -> if (byte == '\r'.code) 3 else 0
                    3 -> if (byte == '\n'.code) return else 0
                    else -> 0
                }
        }
    }
}
