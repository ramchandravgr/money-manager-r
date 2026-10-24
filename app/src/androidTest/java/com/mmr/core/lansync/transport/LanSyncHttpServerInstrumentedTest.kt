package com.mmr.core.lansync.transport

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import java.nio.charset.StandardCharsets

@RunWith(AndroidJUnit4::class)
class LanSyncHttpServerInstrumentedTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var server: LanSyncHttpServer
    private var port: Int = 0

    @Before
    fun startServer() {
        server =
            LanSyncHttpServer(
                scope,
                requestHandler = { _, path, _, _, body ->
                    when {
                        path == LanSyncHttpConstants.PATH_SYNC && body.contains("ping") ->
                            HttpLanResponse.json(200, """{"status":"ok"}""")
                        path == LanSyncHttpConstants.PATH_SYNC ->
                            HttpLanResponse.json(401, """{"error":"unauthorized"}""")
                        else -> HttpLanResponse.json(404, "")
                    }
                },
            )
        server.start(LanSyncHttpConstants.DEFAULT_PORT)
        port = server.port
    }

    @After
    fun stopServer() {
        server.stop()
    }

    @Test
    fun postSync_withBody_returnsJsonFromHandler() =
        runBlocking {
            val response =
                httpPost(
                    path = LanSyncHttpConstants.PATH_SYNC,
                    body = """{"ping":true}""",
                    headers =
                        mapOf(
                            "Content-Type" to "application/json",
                        ),
                )
            assertTrue(response.contains("HTTP/1.1 200"))
            assertTrue(response.contains(""""status":"ok""""))
        }

    @Test
    fun getSync_withoutAuth_returns401() =
        runBlocking {
            val response = httpGet(LanSyncHttpConstants.PATH_SYNC)
            assertTrue(response.contains("HTTP/1.1 401"))
        }

    private fun httpGet(path: String): String = rawRequest("GET", path, "", emptyMap())

    private fun httpPost(
        path: String,
        body: String,
        headers: Map<String, String>,
    ): String = rawRequest("POST", path, body, headers)

    private fun rawRequest(
        method: String,
        path: String,
        body: String,
        extraHeaders: Map<String, String>,
    ): String {
        Socket("127.0.0.1", port).use { socket ->
            val payload = body.toByteArray(StandardCharsets.UTF_8)
            val headerLines =
                buildString {
                    append("$method $path HTTP/1.1\r\n")
                    append("Host: 127.0.0.1\r\n")
                    append("Content-Length: ${payload.size}\r\n")
                    extraHeaders.forEach { (key, value) -> append("$key: $value\r\n") }
                    append("Connection: close\r\n\r\n")
                }
            socket.getOutputStream().write(headerLines.toByteArray(StandardCharsets.UTF_8))
            if (payload.isNotEmpty()) {
                socket.getOutputStream().write(payload)
            }
            socket.getOutputStream().flush()
            return BufferedReader(InputStreamReader(socket.getInputStream())).readText()
        }
    }
}
