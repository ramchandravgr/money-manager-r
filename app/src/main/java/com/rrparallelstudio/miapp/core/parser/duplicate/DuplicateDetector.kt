package com.rrparallelstudio.miapp.core.parser.duplicate

import com.rrparallelstudio.miapp.core.database.dao.SmsMessageRecordDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuplicateDetector
    @Inject
    constructor(
        private val smsMessageRecordDao: SmsMessageRecordDao,
    ) {
        /**
         * Checks if a parsed SMS candidate has already been imported or parsed before
         * by comparing the cryptographic hash against the database.
         */
        suspend fun isDuplicate(messageHash: String): Boolean = smsMessageRecordDao.isDuplicateHash(messageHash)
    }
