package com.rrparallelstudio.miapp.core.webmanage

import android.content.Context
import android.content.res.AssetManager
import com.rrparallelstudio.miapp.core.common.dispatcher.IoDispatcher
import com.rrparallelstudio.miapp.core.data.preferences.AppDisplaySettingsStore
import com.rrparallelstudio.miapp.core.data.preferences.LanSyncIdentityStore
import com.rrparallelstudio.miapp.core.data.preferences.WebManagePreferencesStore
import com.rrparallelstudio.miapp.core.database.entity.AccountEntity
import com.rrparallelstudio.miapp.core.database.entity.CategoryEntity
import com.rrparallelstudio.miapp.core.database.entity.SubcategoryEntity
import com.rrparallelstudio.miapp.core.database.relation.TransactionWithDetails
import com.rrparallelstudio.miapp.core.domain.repository.AccountRepository
import com.rrparallelstudio.miapp.core.domain.repository.ApprovalRepository
import com.rrparallelstudio.miapp.core.domain.repository.CategoryRepository
import com.rrparallelstudio.miapp.core.domain.repository.TransactionRepository
import com.rrparallelstudio.miapp.core.export.PeriodExcelWriter
import com.rrparallelstudio.miapp.core.lansync.transport.HttpLanResponse
import com.rrparallelstudio.miapp.core.loan.LoanLinkedTransactionDeleteMode
import com.rrparallelstudio.miapp.core.model.enums.CategoryType
import com.rrparallelstudio.miapp.core.model.enums.SmsApprovalState
import com.rrparallelstudio.miapp.core.model.enums.TransactionType
import com.rrparallelstudio.miapp.core.transaction.CardRefundLedgerMath
import com.rrparallelstudio.miapp.ui.copy.MmrAccountSmsOriginLabels
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.time.ZoneId
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebManageRequestHandler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val preferencesStore: WebManagePreferencesStore,
        private val sessionManager: WebManageSessionManager,
        private val approvalRepository: ApprovalRepository,
        private val transactionRepository: TransactionRepository,
        private val accountRepository: AccountRepository,
        private val categoryRepository: CategoryRepository,
        private val displaySettingsStore: AppDisplaySettingsStore,
        private val chartThemeSource: WebManageChartThemeSource,
        private val lanSyncIdentityStore: LanSyncIdentityStore,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        fun handle(
            method: String,
            path: String,
            query: String,
            headers: Map<String, String>,
            body: String,
        ): HttpLanResponse {
            if (!preferencesStore.isWebManageEnabled()) {
                return HttpLanResponse.json(403, """{"error":"web_manage_disabled"}""")
            }
            val normalizedPath = normalizeApiPath(path)
            return when {
                normalizedPath == WebManageHttpConstants.PATH_STATUS && method == "GET" -> handleStatus()
                normalizedPath == WebManageHttpConstants.PATH_AUTH_LOGIN && method == "POST" ->
                    handleLogin(body, headers)
                method == "OPTIONS" && normalizedPath.startsWith(WebManageHttpConstants.API_PREFIX) ->
                    HttpLanResponse.json(204, "")
                normalizedPath.startsWith(WebManageHttpConstants.API_PREFIX) ->
                    handleApi(method, normalizedPath, query, headers, body)
                path.startsWith(WebManageHttpConstants.CHART_ASSET_PREFIX) && method == "GET" ->
                    serveBundledAsset("chart", path.removePrefix(WebManageHttpConstants.CHART_ASSET_PREFIX))
                path.startsWith(WebManageHttpConstants.JS_ASSET_PREFIX) && method == "GET" ->
                    serveBundledAsset("js", path.removePrefix(WebManageHttpConstants.JS_ASSET_PREFIX))
                path.startsWith(WebManageHttpConstants.WEB_ASSET_PREFIX) && method == "GET" ->
                    serveAsset(path.removePrefix(WebManageHttpConstants.WEB_ASSET_PREFIX))
                path == "/" && method == "GET" -> serveAsset("index.html")
                else -> HttpLanResponse.json(404, """{"error":"not_found"}""")
            }
        }

        private fun normalizeApiPath(path: String): String = path.trimEnd('/').ifEmpty { "/" }

        private fun handleApi(
            method: String,
            path: String,
            query: String,
            headers: Map<String, String>,
            body: String,
        ): HttpLanResponse {
            val session = headers[WebManageHttpConstants.HEADER_SESSION.lowercase()]
            val clientAddress = WebManageLoginRateLimiter.clientKeyFromHeaders(headers)
            if (!sessionManager.isValid(session, clientAddress)) {
                return HttpLanResponse.json(401, """{"error":"unauthorized"}""")
            }
            return runCatching {
                routeApi(method, path, query, body)
            }.getOrElse {
                HttpLanResponse.json(500, """{"error":"request_failed"}""")
            }
        }

        private fun routeApi(
            method: String,
            path: String,
            query: String,
            body: String,
        ): HttpLanResponse =
            when {
                path == WebManageHttpConstants.PATH_APPROVALS_PENDING && method == "GET" ->
                    runIo { listPendingApprovals() }
                path == WebManageHttpConstants.PATH_APPROVALS_MERGE && method == "POST" ->
                    runIo { mergeApprovals(body) }
                path.matches(Regex("${WebManageHttpConstants.API_PREFIX}/approvals/\\d+/merge-candidates")) &&
                    method == "GET" ->
                    runIo { listApprovalMergeCandidates(path) }
                path.matches(Regex("${WebManageHttpConstants.API_PREFIX}/approvals/\\d+/approve")) &&
                    method == "POST" ->
                    runIo { approve(path, body) }
                path.matches(Regex("${WebManageHttpConstants.API_PREFIX}/approvals/\\d+/reject")) &&
                    method == "POST" ->
                    runIo { reject(path) }
                path.matches(Regex("${WebManageHttpConstants.API_PREFIX}/approvals/\\d+")) &&
                    method == "POST" ->
                    runIo { updateApproval(path, body) }
                path == WebManageHttpConstants.PATH_TRANSACTIONS_EXPORT && method == "GET" ->
                    runIo { exportTransactions(query) }
                path == WebManageHttpConstants.PATH_TRANSACTIONS_BULK_DELETE && method == "POST" ->
                    runIo { bulkDeleteTransactions(body) }
                path == WebManageHttpConstants.PATH_TRANSACTIONS && method == "GET" ->
                    runIo { listTransactions(query) }
                path.matches(Regex("${WebManageHttpConstants.PATH_TRANSACTIONS}/\\d+")) && method == "GET" ->
                    runIo { getTransaction(path) }
                path.matches(Regex("${WebManageHttpConstants.PATH_TRANSACTIONS}/\\d+")) && method == "PUT" ->
                    runIo { updateTransaction(path, body) }
                path.matches(Regex("${WebManageHttpConstants.PATH_TRANSACTIONS}/\\d+")) && method == "DELETE" ->
                    runIo { deleteTransaction(path) }
                path.matches(Regex("${WebManageHttpConstants.PATH_TRANSACTIONS}/\\d+/refund")) && method == "POST" ->
                    runIo { applyTransactionRefund(path, body) }
                path == WebManageHttpConstants.PATH_STATS_SPENDING && method == "GET" ->
                    runIo { spendingStats(query) }
                path == WebManageHttpConstants.PATH_STATS_CHART && method == "GET" ->
                    runIo { spendingChart(query) }
                path == WebManageHttpConstants.PATH_META_ACCOUNTS && method == "GET" ->
                    runIo { metaAccounts() }
                path == WebManageHttpConstants.PATH_META_CATEGORIES && method == "GET" ->
                    runIo { metaCategories() }
                path == WebManageHttpConstants.PATH_META_CATEGORIES && method == "POST" ->
                    runIo { createCategory(body) }
                path == WebManageHttpConstants.PATH_META_SUBCATEGORIES && method == "POST" ->
                    runIo { createSubcategory(body) }
                else -> HttpLanResponse.json(404, """{"error":"not_found"}""")
            }

        private fun handleStatus(): HttpLanResponse {
            val display = chartThemeSource.snapshot()
            return HttpLanResponse.json(
                200,
                JSONObject()
                    .put("enabled", preferencesStore.isWebManageEnabled())
                    .put("requiresPassword", preferencesStore.hasAccessPassword())
                    .put("appearance", display.webAppearance)
                    .toString(),
            )
        }

        private fun handleLogin(
            body: String,
            headers: Map<String, String>,
        ): HttpLanResponse {
            val clientKey = WebManageLoginRateLimiter.clientKeyFromHeaders(headers)
            if (!WebManageLoginRateLimiter.isLoginAllowed(clientKey)) {
                return HttpLanResponse.json(429, """{"error":"too_many_attempts"}""")
            }
            if (!preferencesStore.hasAccessPassword()) {
                return HttpLanResponse.json(403, """{"error":"password_not_set"}""")
            }
            val json =
                runCatching { JSONObject(body) }.getOrNull()
                    ?: return HttpLanResponse.json(400, """{"error":"invalid_body"}""")
            val password = json.optString("password").trim()
            if (password.isBlank()) {
                return HttpLanResponse.json(400, """{"error":"password_required"}""")
            }
            val salt =
                preferencesStore.getPasswordSalt()
                    ?: return HttpLanResponse.json(403, """{"error":"password_not_set"}""")
            val hash =
                preferencesStore.getPasswordHash()
                    ?: return HttpLanResponse.json(403, """{"error":"password_not_set"}""")
            if (!WebManagePasswordHasher.verify(password, salt, hash)) {
                WebManageLoginRateLimiter.recordFailure(clientKey)
                return HttpLanResponse.json(401, """{"error":"invalid_password"}""")
            }
            WebManageLoginRateLimiter.recordSuccess(clientKey)
            val token = sessionManager.createSession(clientKey)
            return HttpLanResponse.json(
                200,
                JSONObject().put("token", token).toString(),
            )
        }

        private suspend fun listPendingApprovals(): HttpLanResponse {
            val rows =
                approvalRepository
                    .getApprovalsByStateWithSms(SmsApprovalState.PENDING)
                    .first()
            val array = JSONArray()
            rows.forEach { row ->
                val approval = row.approval
                array.put(
                    JSONObject()
                        .put("id", approval.id)
                        .put("amount", approval.extractedAmount)
                        .put("type", approval.extractedType.name)
                        .put("dateTimeMillis", approval.extractedDateTime)
                        .put("sender", approval.extractedSender)
                        .put("merchant", approval.extractedMerchant)
                        .put("note", approval.extractedNote)
                        .put("categoryId", approval.extractedCategoryId)
                        .put("accountId", approval.extractedAccountId)
                        .put("smsBody", row.smsBody.take(2000)),
                )
            }
            return HttpLanResponse.json(200, JSONObject().put("items", array).toString())
        }

        private suspend fun approve(
            path: String,
            body: String,
        ): HttpLanResponse {
            val id = path.approvalId() ?: return HttpLanResponse.json(400, """{"error":"invalid_id"}""")
            val account =
                accountRepository.getDefaultAccount()
                    ?: accountRepository.getActiveAccounts().first().firstOrNull()
                    ?: return HttpLanResponse.json(409, """{"error":"no_account"}""")
            val approval =
                approvalRepository.getApprovalById(id)
                    ?: return HttpLanResponse.json(404, """{"error":"not_found"}""")
            val json = runCatching { JSONObject(body.ifBlank { "{}" }) }.getOrElse { JSONObject() }
            if (json.length() > 0) {
                val type =
                    runCatching { TransactionType.valueOf(json.optString("type")) }.getOrNull()
                        ?: approval.extractedType
                approvalRepository.updateApproval(
                    id = id,
                    amount = json.optDouble("amount", approval.extractedAmount),
                    type = type,
                    categoryId = json.optLong("categoryId").takeIf { it > 0 } ?: approval.extractedCategoryId,
                    accountId = json.optLong("accountId").takeIf { it > 0 } ?: approval.extractedAccountId,
                    note = json.optString("note").takeIf { it.isNotBlank() } ?: approval.extractedNote,
                    dateTimeMillis = json.optLong("dateTimeMillis").takeIf { it > 0 },
                )
            }
            val refreshed = approvalRepository.getApprovalById(id) ?: approval
            val categoryId = refreshed.extractedCategoryId
            val txnId =
                approvalRepository.approveCandidate(
                    id = id,
                    fallbackAccountId = refreshed.extractedAccountId ?: account.id,
                    fallbackCategoryId = categoryId,
                    subcategoryId = json.optLong("subcategoryId").takeIf { it > 0 },
                    relatedAccountId = json.optLong("relatedAccountId").takeIf { it > 0 },
                )
            return HttpLanResponse.json(200, JSONObject().put("transactionId", txnId).toString())
        }

        private suspend fun listApprovalMergeCandidates(path: String): HttpLanResponse {
            val id = path.approvalId() ?: return HttpLanResponse.json(400, """{"error":"invalid_id"}""")
            val candidates = approvalRepository.findMergeCandidatesForApproval(id)
            val array = JSONArray()
            candidates.forEach { candidate ->
                array.put(
                    JSONObject()
                        .put("transactionId", candidate.transactionId)
                        .put("amount", candidate.amount)
                        .put("dateTimeMillis", candidate.dateTimeMillis)
                        .put("note", candidate.note),
                )
            }
            return HttpLanResponse.json(200, JSONObject().put("items", array).toString())
        }

        private suspend fun mergeApprovals(body: String): HttpLanResponse {
            val json =
                runCatching { JSONObject(body) }.getOrNull()
                    ?: return HttpLanResponse.json(400, """{"error":"invalid_body"}""")
            val idsArray =
                json.optJSONArray("approvalIds")
                    ?: return HttpLanResponse.json(400, """{"error":"approval_ids_required"}""")
            val approvalIds = (0 until idsArray.length()).mapNotNull { idsArray.optLong(it).takeIf { it > 0 } }
            if (approvalIds.isEmpty()) {
                return HttpLanResponse.json(400, """{"error":"approval_ids_required"}""")
            }
            val transactionId = json.optLong("transactionId")
            if (transactionId <= 0L) {
                return HttpLanResponse.json(400, """{"error":"transaction_id_required"}""")
            }
            val categoryId = json.optLong("categoryId").takeIf { it > 0 }
            val subcategoryId = json.optLong("subcategoryId").takeIf { it > 0 }
            val useLedgerCategory = json.optBoolean("useLedgerCategory", false)
            val txnId =
                approvalRepository.mergeApprovalsWithTransaction(
                    approvalIds = approvalIds,
                    transactionId = transactionId,
                    categoryId = categoryId,
                    subcategoryId = subcategoryId,
                    useLedgerCategory = useLedgerCategory,
                )
            return HttpLanResponse.json(200, JSONObject().put("transactionId", txnId).toString())
        }

        private suspend fun createCategory(body: String): HttpLanResponse {
            val json =
                runCatching { JSONObject(body) }.getOrNull()
                    ?: return HttpLanResponse.json(400, """{"error":"invalid_body"}""")
            val name = json.optString("name").trim()
            if (name.isEmpty()) return HttpLanResponse.json(400, """{"error":"name_required"}""")
            val type =
                runCatching { CategoryType.valueOf(json.optString("type", "EXPENSE")) }.getOrNull()
                    ?: CategoryType.EXPENSE
            val id =
                categoryRepository.createCategory(
                    CategoryEntity(name = name, type = type),
                )
            return HttpLanResponse.json(
                200,
                JSONObject()
                    .put("id", id)
                    .put("name", name)
                    .put("type", type.name)
                    .toString(),
            )
        }

        private suspend fun createSubcategory(body: String): HttpLanResponse {
            val json =
                runCatching { JSONObject(body) }.getOrNull()
                    ?: return HttpLanResponse.json(400, """{"error":"invalid_body"}""")
            val categoryId = json.optLong("categoryId")
            if (categoryId <= 0L) return HttpLanResponse.json(400, """{"error":"category_id_required"}""")
            val name = json.optString("name").trim()
            if (name.isEmpty()) return HttpLanResponse.json(400, """{"error":"name_required"}""")
            val id =
                categoryRepository.createSubcategory(
                    SubcategoryEntity(categoryId = categoryId, name = name),
                )
            return HttpLanResponse.json(
                200,
                JSONObject()
                    .put("id", id)
                    .put("categoryId", categoryId)
                    .put("name", name)
                    .toString(),
            )
        }

        private suspend fun reject(path: String): HttpLanResponse {
            val id = path.approvalId() ?: return HttpLanResponse.json(400, """{"error":"invalid_id"}""")
            approvalRepository.rejectCandidate(id)
            return HttpLanResponse.json(200, """{"status":"rejected"}""")
        }

        private suspend fun updateApproval(
            path: String,
            body: String,
        ): HttpLanResponse {
            val id = path.approvalId() ?: return HttpLanResponse.json(400, """{"error":"invalid_id"}""")
            val json =
                runCatching { JSONObject(body) }.getOrNull()
                    ?: return HttpLanResponse.json(400, """{"error":"invalid_body"}""")
            val type =
                runCatching { TransactionType.valueOf(json.optString("type")) }.getOrNull()
                    ?: return HttpLanResponse.json(400, """{"error":"invalid_type"}""")
            approvalRepository.updateApproval(
                id = id,
                amount = json.optDouble("amount"),
                type = type,
                categoryId = json.optLong("categoryId").takeIf { it > 0 },
                accountId = json.optLong("accountId").takeIf { it > 0 },
                note = json.optString("note").takeIf { it.isNotBlank() },
                dateTimeMillis = json.optLong("dateTimeMillis").takeIf { it > 0 },
            )
            return HttpLanResponse.json(200, """{"status":"updated"}""")
        }

        private data class TransactionListQuery(
            val rangeStart: Long,
            val rangeEnd: Long,
        )

        private suspend fun fetchFilteredTransactions(
            query: String,
        ): Pair<List<TransactionWithDetails>, TransactionListQuery> {
            val params = query.parseQueryParams()
            val start = params["start"]?.toLongOrNull()
            val end = params["end"]?.toLongOrNull()
            val now = System.currentTimeMillis()
            val rangeStart = start ?: (now - 30L * 24 * 60 * 60 * 1000)
            val rangeEnd = end ?: now
            val type =
                params["type"]?.takeIf { it.isNotBlank() }?.let {
                    runCatching { TransactionType.valueOf(it) }.getOrNull()
                }
            val accountId = params["accountId"]?.toLongOrNull()?.takeIf { it > 0 }
            val categoryId = params["categoryId"]?.toLongOrNull()?.takeIf { it > 0 }
            val searchQuery = params["q"]?.trim()?.takeIf { it.isNotEmpty() }
            val all =
                if (searchQuery != null) {
                    transactionRepository
                        .searchTransactions(searchQuery)
                        .first()
                        .filter { row ->
                            val txn = row.transaction
                            txn.dateTime in rangeStart..rangeEnd &&
                                (type == null || txn.type == type) &&
                                (categoryId == null || txn.categoryId == categoryId) &&
                                (accountId == null || txn.accountId == accountId || txn.relatedAccountId == accountId)
                        }
                } else {
                    transactionRepository
                        .getFilteredTransactions(
                            startDate = rangeStart,
                            endDate = rangeEnd,
                            accountId = null,
                            categoryId = categoryId,
                            subcategoryId = null,
                            type = type,
                        ).first()
                        .filter { row ->
                            val txn = row.transaction
                            accountId == null || txn.accountId == accountId || txn.relatedAccountId == accountId
                        }
                }
            return all to TransactionListQuery(rangeStart, rangeEnd)
        }

        private fun totalsForRows(rows: List<TransactionWithDetails>): Pair<Double, Double> {
            var incomeTotal = 0.0
            var expenseTotal = 0.0
            rows.forEach { row ->
                when (row.transaction.type) {
                    TransactionType.INCOME -> incomeTotal += row.transaction.amount
                    TransactionType.EXPENSE -> expenseTotal += row.transaction.amount
                    TransactionType.TRANSFER,
                    TransactionType.LOAN_PAYMENT,
                    TransactionType.ADJUSTMENT,
                    -> Unit
                }
            }
            return incomeTotal to expenseTotal
        }

        private suspend fun listTransactions(query: String): HttpLanResponse {
            val params = query.parseQueryParams()
            val limit = params["limit"]?.toIntOrNull()?.coerceIn(1, 500) ?: 200
            val offset = params["offset"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val (all, bounds) = fetchFilteredTransactions(query)
            val slice = all.drop(offset).take(limit)
            val (incomeTotal, expenseTotal) = totalsForRows(all)
            val array = JSONArray()
            slice.forEach { row -> array.put(row.toJson()) }
            return HttpLanResponse.json(
                200,
                JSONObject()
                    .put("total", all.size)
                    .put("totalIncome", incomeTotal)
                    .put("totalExpense", expenseTotal)
                    .put("startMillis", bounds.rangeStart)
                    .put("endMillis", bounds.rangeEnd)
                    .put("items", array)
                    .toString(),
            )
        }

        private suspend fun exportTransactions(query: String): HttpLanResponse {
            val (all, _) = fetchFilteredTransactions(query)
            val display = displaySettingsStore.snapshot()
            val output = ByteArrayOutputStream()
            PeriodExcelWriter.write(
                items = all,
                zoneId = ZoneId.systemDefault(),
                currencySymbol = display.mainCurrencySymbol,
                currencyCode = display.mainCurrencyCode,
                applicationName = context.getString(com.rrparallelstudio.miapp.R.string.app_name),
                sheetName = "Web export",
                output = output,
            )
            return HttpLanResponse(
                status = 200,
                body = output.toByteArray(),
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                extraHeaders =
                    mapOf(
                        "Content-Disposition" to "attachment; filename=\"mmr-transactions.xlsx\"",
                    ),
            )
        }

        private suspend fun bulkDeleteTransactions(body: String): HttpLanResponse {
            val json =
                runCatching { JSONObject(body) }.getOrNull()
                    ?: return HttpLanResponse.json(400, """{"error":"invalid_body"}""")
            val idsArray =
                json.optJSONArray("ids")
                    ?: return HttpLanResponse.json(400, """{"error":"ids_required"}""")
            val ids =
                (0 until idsArray.length())
                    .mapNotNull { idsArray.optLong(it).takeIf { it > 0 } }
                    .distinct()
            if (ids.isEmpty()) {
                return HttpLanResponse.json(400, """{"error":"ids_required"}""")
            }
            var deleted = 0
            val failures = JSONArray()
            for (id in ids) {
                val existing = transactionRepository.getTransactionDetailedById(id)
                if (existing == null) {
                    failures.put(JSONObject().put("id", id).put("error", "not_found"))
                    continue
                }
                runCatching {
                    val mode =
                        if (transactionRepository.hasLinkedLoanPayment(id)) {
                            LoanLinkedTransactionDeleteMode.REVERSE
                        } else {
                            LoanLinkedTransactionDeleteMode.UNLINK
                        }
                    transactionRepository.deleteTransactionWithLoanPolicy(existing.transaction, mode)
                }.onSuccess { deleted++ }
                    .onFailure {
                        failures.put(
                            JSONObject()
                                .put("id", id)
                                .put("error", it.message ?: "delete_failed"),
                        )
                    }
            }
            return HttpLanResponse.json(
                200,
                JSONObject()
                    .put("deleted", deleted)
                    .put("failed", failures.length())
                    .put("failures", failures)
                    .toString(),
            )
        }

        private suspend fun getTransaction(path: String): HttpLanResponse {
            val id = path.transactionId() ?: return HttpLanResponse.json(400, """{"error":"invalid_id"}""")
            val row =
                transactionRepository.getTransactionDetailedById(id)
                    ?: return HttpLanResponse.json(404, """{"error":"not_found"}""")
            return HttpLanResponse.json(200, row.toJson().toString())
        }

        private suspend fun updateTransaction(
            path: String,
            body: String,
        ): HttpLanResponse {
            val id = path.transactionId() ?: return HttpLanResponse.json(400, """{"error":"invalid_id"}""")
            val existing =
                transactionRepository.getTransactionDetailedById(id)
                    ?: return HttpLanResponse.json(404, """{"error":"not_found"}""")
            val json =
                runCatching { JSONObject(body) }.getOrNull()
                    ?: return HttpLanResponse.json(400, """{"error":"invalid_body"}""")
            val type =
                json.optString("type").takeIf { it.isNotBlank() }?.let {
                    runCatching { TransactionType.valueOf(it) }.getOrNull()
                } ?: existing.transaction.type
            val updated =
                existing.transaction.copy(
                    amount = json.optDouble("amount", existing.transaction.amount),
                    type = type,
                    categoryId =
                        if (json.has("categoryId")) {
                            json.optLong("categoryId").takeIf { it > 0 }
                        } else {
                            existing.transaction.categoryId
                        },
                    subcategoryId =
                        if (json.has("subcategoryId")) {
                            json.optLong("subcategoryId").takeIf { it > 0 }
                        } else {
                            existing.transaction.subcategoryId
                        },
                    accountId =
                        if (json.has("accountId")) {
                            json.optLong("accountId").takeIf { it > 0 } ?: existing.transaction.accountId
                        } else {
                            existing.transaction.accountId
                        },
                    relatedAccountId =
                        if (json.has("relatedAccountId")) {
                            json.optLong("relatedAccountId").takeIf { it > 0 }
                        } else {
                            existing.transaction.relatedAccountId
                        },
                    note = if (json.has("note")) json.optString("note") else existing.transaction.note,
                    dateTime = json.optLong("dateTimeMillis", existing.transaction.dateTime),
                    updatedAt = System.currentTimeMillis(),
                )
            transactionRepository.updateTransaction(updated)
            return HttpLanResponse.json(200, """{"status":"updated"}""")
        }

        private suspend fun applyTransactionRefund(
            path: String,
            body: String,
        ): HttpLanResponse {
            val id = path.transactionId() ?: return HttpLanResponse.json(400, """{"error":"invalid_id"}""")
            val existing =
                transactionRepository.getTransactionDetailedById(id)
                    ?: return HttpLanResponse.json(404, """{"error":"not_found"}""")
            val txn = existing.transaction
            if (txn.type != TransactionType.EXPENSE) {
                return HttpLanResponse.json(400, """{"error":"expense_only"}""")
            }
            val json =
                runCatching { JSONObject(body) }.getOrNull()
                    ?: return HttpLanResponse.json(400, """{"error":"invalid_body"}""")
            val refundAmount = json.optDouble("refundAmount", 0.0)
            if (refundAmount <= 0.0 || !refundAmount.isFinite()) {
                return HttpLanResponse.json(400, """{"error":"refund_amount_required"}""")
            }
            val math =
                runCatching {
                    CardRefundLedgerMath.applyRefund(
                        currentNetAmount = txn.amount,
                        currentOriginalAmount = txn.originalAmount,
                        currentRefundedTotal = txn.refundedTotal,
                        refundAmount = refundAmount,
                    )
                }.getOrElse {
                    return HttpLanResponse.json(
                        400,
                        JSONObject().put("error", "invalid_refund").put("message", it.message).toString(),
                    )
                }
            transactionRepository.updateTransaction(
                txn.copy(
                    amount = math.netAmount,
                    originalAmount = math.originalAmount,
                    refundedTotal = math.refundedTotal,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            val refreshed =
                transactionRepository.getTransactionDetailedById(id)
                    ?: return HttpLanResponse.json(500, """{"error":"refresh_failed"}""")
            return HttpLanResponse.json(200, refreshed.toJson().toString())
        }

        private suspend fun deleteTransaction(path: String): HttpLanResponse {
            val id = path.transactionId() ?: return HttpLanResponse.json(400, """{"error":"invalid_id"}""")
            val existing =
                transactionRepository.getTransactionDetailedById(id)
                    ?: return HttpLanResponse.json(404, """{"error":"not_found"}""")
            val mode =
                if (transactionRepository.hasLinkedLoanPayment(id)) {
                    LoanLinkedTransactionDeleteMode.REVERSE
                } else {
                    LoanLinkedTransactionDeleteMode.UNLINK
                }
            transactionRepository.deleteTransactionWithLoanPolicy(existing.transaction, mode)
            return HttpLanResponse.json(200, """{"status":"deleted"}""")
        }

        private suspend fun spendingChart(query: String): HttpLanResponse {
            val bounds = parseStatsBounds(query)
            val ledger = parseStatsLedger(query)
            val (rows, tabTotal, noDataMessage) = categoryBreakdownForLedger(ledger, bounds.start, bounds.end)
            val chartTheme = chartThemeSource.snapshot().chartTheme
            val chartPayload =
                JSONObject(
                    WebManageChartJsonBuilder.expensePieChartJson(
                        spendings = rows,
                        tabTotal = tabTotal,
                        noDataMessage = noDataMessage,
                        theme = chartTheme,
                    ),
                )
            chartPayload
                .put("ledger", ledger)
                .put("mode", bounds.mode)
                .put("startMillis", bounds.start)
                .put("endMillis", bounds.end)
            return HttpLanResponse.json(200, chartPayload.toString())
        }

        private suspend fun spendingStats(query: String): HttpLanResponse {
            val bounds = parseStatsBounds(query)
            val ledger = parseStatsLedger(query)
            val incomeTotal = transactionRepository.getTotalIncomeBetween(bounds.start, bounds.end).first() ?: 0.0
            val expenseTotal = transactionRepository.getTotalExpenseBetween(bounds.start, bounds.end).first() ?: 0.0
            val (rows, _, _) = categoryBreakdownForLedger(ledger, bounds.start, bounds.end)
            val array = JSONArray()
            rows.forEach { row ->
                array.put(
                    JSONObject()
                        .put("categoryId", row.categoryId)
                        .put("categoryName", row.categoryName)
                        .put("amount", row.totalAmount),
                )
            }
            return HttpLanResponse.json(
                200,
                JSONObject()
                    .put("mode", bounds.mode)
                    .put("ledger", ledger)
                    .put("startMillis", bounds.start)
                    .put("endMillis", bounds.end)
                    .put("totalIncome", incomeTotal)
                    .put("totalExpense", expenseTotal)
                    .put("net", incomeTotal - expenseTotal)
                    .put("categories", array)
                    .toString(),
            )
        }

        private suspend fun categoryBreakdownForLedger(
            ledger: String,
            start: Long,
            end: Long,
        ): Triple<List<com.rrparallelstudio.miapp.core.database.model.CategorySpending>, Double, String> =
            if (ledger == STATS_LEDGER_INCOME) {
                val total = transactionRepository.getTotalIncomeBetween(start, end).first() ?: 0.0
                val rows = transactionRepository.getCategoryIncomeBetween(start, end).first()
                Triple(rows, total, "No income in this period")
            } else {
                val total = transactionRepository.getTotalExpenseBetween(start, end).first() ?: 0.0
                val rows = transactionRepository.getCategorySpendingBetween(start, end).first()
                Triple(rows, total, "No expenses in this period")
            }

        private data class StatsBounds(
            val mode: String,
            val start: Long,
            val end: Long,
        )

        private fun parseStatsBounds(query: String): StatsBounds {
            val params = query.parseQueryParams()
            val customStart = params["start"]?.toLongOrNull()
            val customEnd = params["end"]?.toLongOrNull()
            if (customStart != null && customEnd != null && customStart <= customEnd) {
                return StatsBounds(mode = "custom", start = customStart, end = customEnd)
            }
            val mode = params["mode"] ?: "month"
            val (start, end) = spendingRange(mode)
            return StatsBounds(mode = mode, start = start, end = end)
        }

        private fun parseStatsLedger(query: String): String {
            val ledger = query.parseQueryParams()["ledger"]?.lowercase() ?: STATS_LEDGER_EXPENSE
            return if (ledger == STATS_LEDGER_INCOME) STATS_LEDGER_INCOME else STATS_LEDGER_EXPENSE
        }

        companion object {
            private const val STATS_LEDGER_EXPENSE = "expense"
            private const val STATS_LEDGER_INCOME = "income"
        }

        private suspend fun metaAccounts(): HttpLanResponse {
            val accounts = accountRepository.getActiveAccounts().first()
            val array = JSONArray()
            accounts.forEach { account ->
                array.put(account.toMetaJson())
            }
            return HttpLanResponse.json(200, JSONObject().put("items", array).toString())
        }

        private suspend fun metaCategories(): HttpLanResponse {
            val tree = categoryRepository.observeActiveCategoryTree().first()
            val array = JSONArray()
            tree.forEach { parent ->
                val subs = JSONArray()
                parent.subcategories.forEach { sub ->
                    subs.put(
                        JSONObject()
                            .put("id", sub.id.value)
                            .put("name", sub.name),
                    )
                }
                array.put(
                    JSONObject()
                        .put("id", parent.category.id.value)
                        .put("name", parent.category.name)
                        .put("type", parent.category.type.name)
                        .put("subcategories", subs),
                )
            }
            return HttpLanResponse.json(200, JSONObject().put("items", array).toString())
        }

        private fun serveAsset(relativePath: String): HttpLanResponse =
            serveBundledAsset("web", relativePath.trimStart('/').ifBlank { "index.html" })

        private fun serveBundledAsset(
            root: String,
            relativePath: String,
        ): HttpLanResponse {
            val safe = relativePath.trimStart('/')
            if (safe.contains("..") || safe.isBlank()) {
                return HttpLanResponse.json(403, """{"error":"forbidden"}""")
            }
            val assetPath = "$root/$safe"
            val assets: AssetManager = context.assets
            val bytes =
                runCatching {
                    assets.open(assetPath).use { it.readBytes() }
                }.getOrNull() ?: return HttpLanResponse.json(404, """{"error":"not_found"}""")
            val contentType = contentTypeFor(safe)
            return HttpLanResponse.bytes(200, bytes, contentType)
        }

        private fun contentTypeFor(path: String): String =
            when {
                path.endsWith(".html") -> "text/html; charset=utf-8"
                path.endsWith(".js") -> "application/javascript; charset=utf-8"
                path.endsWith(".css") -> "text/css; charset=utf-8"
                path.endsWith(".svg") -> "image/svg+xml"
                path.endsWith(".png") -> "image/png"
                else -> "application/octet-stream"
            }

        private fun runIo(block: suspend () -> HttpLanResponse): HttpLanResponse = runBlocking(ioDispatcher) { block() }

        private fun spendingRange(mode: String): Pair<Long, Long> {
            val endCal = Calendar.getInstance()
            endOfDay(endCal)
            val end = endCal.timeInMillis
            val startCal = Calendar.getInstance()
            when (mode.lowercase()) {
                "week" -> {
                    startCal.timeInMillis = end
                    startCal.add(Calendar.DAY_OF_YEAR, -6)
                    startOfDay(startCal)
                }
                "year" -> {
                    startCal.timeInMillis = end
                    startCal.set(Calendar.DAY_OF_YEAR, 1)
                    startOfDay(startCal)
                }
                else -> {
                    startCal.timeInMillis = end
                    startCal.set(Calendar.DAY_OF_MONTH, 1)
                    startOfDay(startCal)
                }
            }
            return startCal.timeInMillis to end
        }

        private fun startOfDay(cal: Calendar) {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }

        private fun endOfDay(cal: Calendar) {
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
        }

        private fun AccountEntity.toMetaJson(): JSONObject {
            val originLabel = MmrAccountSmsOriginLabels.labelFor(this)
            return JSONObject()
                .put("id", id)
                .put("name", name)
                .put("type", type.name)
                .put("smsAutoCreated", smsAutoCreated)
                .put("smsAutoCreatedUserModified", smsAutoCreatedUserModified)
                .apply {
                    if (originLabel != null) {
                        put("smsOriginLabel", originLabel)
                    }
                }
        }

        private fun com.rrparallelstudio.miapp.core.database.relation.TransactionWithDetails.toJson(): JSONObject {
            val txn = transaction
            val createdByLabel = formatCreatedByLabel(txn.createdByProfileId, txn.createdByDisplayName)
            return JSONObject()
                .put("id", txn.id)
                .put("amount", txn.amount)
                .put("type", txn.type.name)
                .put("dateTimeMillis", txn.dateTime)
                .put("note", txn.note)
                .put("categoryId", txn.categoryId)
                .put("categoryName", category?.name)
                .put("subcategoryId", txn.subcategoryId)
                .put("subcategoryName", subcategory?.name)
                .put("accountId", txn.accountId)
                .put("accountName", account?.name)
                .put("relatedAccountId", txn.relatedAccountId)
                .put("relatedAccountName", relatedAccount?.name)
                .put("originalAmount", txn.originalAmount)
                .put("refundedTotal", txn.refundedTotal)
                .apply {
                    if (createdByLabel != null) {
                        put("createdByLabel", createdByLabel)
                    }
                    val gross = txn.originalAmount
                    if (gross != null && txn.refundedTotal > 0.0) {
                        put(
                            "refundSummary",
                            "Original $gross · Refunded ${txn.refundedTotal} · Net ${txn.amount}",
                        )
                    }
                }
        }

        private fun formatCreatedByLabel(
            profileId: String?,
            snapshotName: String?,
        ): String? {
            if (profileId.isNullOrBlank()) return null
            if (profileId == lanSyncIdentityStore.ownerProfileId) return null
            val name = snapshotName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return "Created by $name"
        }

        private fun String.approvalId(): Long? =
            Regex("/approvals/(\\d+)")
                .find(this)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()

        private fun String.transactionId(): Long? =
            Regex("/transactions/(\\d+)")
                .find(this)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()

        private fun String.parseQueryParams(): Map<String, String> {
            if (isEmpty()) return emptyMap()
            return split('&')
                .mapNotNull { part ->
                    val pieces = part.split('=', limit = 2)
                    if (pieces.size == 2) pieces[0] to pieces[1] else null
                }.toMap()
        }
    }
