package com.mmr.core.webmanage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mmr.core.data.preferences.AppDisplaySettingsStore
import com.mmr.core.data.preferences.LanSyncIdentityStore
import com.mmr.core.data.preferences.WebManagePreferencesStore
import com.mmr.core.data.repository.CategoryRepositoryImpl
import com.mmr.core.data.repository.MmrInstrumentedRepositoryGraph
import com.mmr.core.lansync.LanSyncTombstoneRecorder
import com.mmr.core.lansync.transport.LanSyncHttpConstants
import com.mmr.core.lansync.transport.LanSyncHttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
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
class WebManageHttpServerInstrumentedTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var graph: MmrInstrumentedRepositoryGraph
    private lateinit var prefs: WebManagePreferencesStore
    private lateinit var handler: WebManageRequestHandler
    private lateinit var server: LanSyncHttpServer
    private var port: Int = 0

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        graph = MmrInstrumentedRepositoryGraph(context)
        prefs = WebManagePreferencesStore(context)
        prefs.setWebManageEnabled(false)
        val salt = WebManagePasswordHasher.newSalt()
        prefs.setAccessPassword(salt, WebManagePasswordHasher.hash("test-pass-123", salt))
        prefs.setWebManageEnabled(true)

        val categoryRepository =
            CategoryRepositoryImpl(
                graph.database.categoryDao(),
                graph.database.transactionDao(),
                graph.database.recurringRuleDao(),
                LanSyncTombstoneRecorder(
                    graph.database.syncTombstoneDao(),
                    LanSyncIdentityStore(context),
                ),
            )
        handler =
            WebManageRequestHandler(
                context = context,
                preferencesStore = prefs,
                sessionManager = WebManageSessionManager(),
                approvalRepository = graph.approvalRepository,
                transactionRepository = graph.transactionRepository,
                accountRepository = graph.accountRepository,
                categoryRepository = categoryRepository,
                displaySettingsStore = AppDisplaySettingsStore(context),
                ioDispatcher = Dispatchers.IO,
            )
        server =
            LanSyncHttpServer(
                scope,
                requestHandler = { method, path, query, headers, body ->
                    handler.handle(method, path, query, headers, body)
                },
            )
        server.start(LanSyncHttpConstants.DEFAULT_PORT)
        port = server.port
    }

    @After
    fun tearDown() {
        server.stop()
        prefs.setWebManageEnabled(false)
        graph.close()
    }

    @Test
    fun status_whenEnabled_reportsRequiresPassword() =
        runBlocking {
            val body = httpGet(WebManageHttpConstants.PATH_STATUS)
            assertTrue(body.contains("HTTP/1.1 200"))
            val jsonStart = body.indexOf('{')
            val json = JSONObject(body.substring(jsonStart))
            assertTrue(json.getBoolean("enabled"))
            assertTrue(json.getBoolean("requiresPassword"))
        }

    @Test
    fun login_withValidPassword_returnsSessionToken() =
        runBlocking {
            val response =
                httpPost(
                    WebManageHttpConstants.PATH_AUTH_LOGIN,
                    """{"password":"test-pass-123"}""",
                    emptyMap(),
                )
            assertTrue(response.contains("HTTP/1.1 200"))
            val json = JSONObject(response.substring(response.indexOf('{')))
            assertTrue(json.getString("token").isNotBlank())
        }

    @Test
    fun pendingApprovals_withoutSession_returns401() =
        runBlocking {
            val response = httpGet(WebManageHttpConstants.PATH_APPROVALS_PENDING)
            assertTrue(response.contains("HTTP/1.1 401"))
        }

    @Test
    fun apiRoute_isNotServedAsStaticWebAsset() =
        runBlocking {
            val response = httpGet(WebManageHttpConstants.PATH_APPROVALS_PENDING)
            assertTrue(response.contains("HTTP/1.1 401"))
        }

    @Test
    fun statsChart_withSession_returnsPiePayload() =
        runBlocking {
            val loginBody =
                httpPost(
                    WebManageHttpConstants.PATH_AUTH_LOGIN,
                    """{"password":"test-pass-123"}""",
                    emptyMap(),
                )
            val token = JSONObject(loginBody.substring(loginBody.indexOf('{'))).getString("token")
            val response =
                httpGet(
                    WebManageHttpConstants.PATH_STATS_CHART + "?mode=month",
                    mapOf(WebManageHttpConstants.HEADER_SESSION to token),
                )
            assertTrue(response.contains("HTTP/1.1 200"))
            val json = JSONObject(response.substring(response.indexOf('{')))
            assertTrue(json.has("series"))
        }

    private fun httpGet(
        path: String,
        extraHeaders: Map<String, String> = emptyMap(),
    ): String = rawRequest("GET", path, "", extraHeaders)

    private fun httpPost(
        path: String,
        body: String,
        extraHeaders: Map<String, String>,
    ): String = rawRequest("POST", path, body, extraHeaders)

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
