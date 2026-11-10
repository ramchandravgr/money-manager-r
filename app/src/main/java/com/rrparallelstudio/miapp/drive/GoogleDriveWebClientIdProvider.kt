package com.rrparallelstudio.miapp.drive

import com.rrparallelstudio.miapp.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveWebClientIdProvider
    @Inject
    constructor() {
        val clientId: String = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        val isConfigured: Boolean = clientId.isNotEmpty()
    }
