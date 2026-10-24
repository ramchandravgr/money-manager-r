package com.mmr.core.webmanage

object WebManageHttpConstants {
    const val HEADER_SESSION = "X-MMR-Web-Session"
    const val API_PREFIX = "/mmr/web/v1/api"
    const val WEB_ASSET_PREFIX = "/mmr/web/"
    const val PATH_STATUS = "$API_PREFIX/status"
    const val PATH_AUTH_LOGIN = "$API_PREFIX/auth/login"
    const val PATH_APPROVALS_PENDING = "$API_PREFIX/approvals/pending"
    const val PATH_TRANSACTIONS = "$API_PREFIX/transactions"
    const val PATH_TRANSACTIONS_EXPORT = "$API_PREFIX/transactions/export"
    const val PATH_TRANSACTIONS_BULK_DELETE = "$API_PREFIX/transactions/bulk-delete"
    const val PATH_STATS_SPENDING = "$API_PREFIX/stats/spending"
    const val PATH_STATS_CHART = "$API_PREFIX/stats/chart"
    const val CHART_ASSET_PREFIX = "/mmr/web/chart/"
    const val JS_ASSET_PREFIX = "/mmr/web/js/"
    const val PATH_META_ACCOUNTS = "$API_PREFIX/meta/accounts"
    const val PATH_META_CATEGORIES = "$API_PREFIX/meta/categories"
    const val PATH_META_SUBCATEGORIES = "$API_PREFIX/meta/subcategories"
    const val PATH_APPROVALS_MERGE = "$API_PREFIX/approvals/merge"
}
