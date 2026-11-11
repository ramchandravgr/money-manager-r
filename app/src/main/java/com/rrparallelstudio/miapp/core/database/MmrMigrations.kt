package com.rrparallelstudio.miapp.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object MmrMigrations {
    val MIGRATION_26_27 =
        object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE lan_sync_log_entries ADD COLUMN unchangedCount INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE lan_sync_log_entries ADD COLUMN addedCount INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE lan_sync_log_entries ADD COLUMN overridesJson TEXT NOT NULL DEFAULT '[]'",
                )
            }
        }

    val MIGRATION_27_28 =
        object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sms_account_overrides (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        templateId TEXT NOT NULL,
                        instrument TEXT NOT NULL,
                        smsLastFour TEXT NOT NULL,
                        accountId INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_sms_account_overrides_templateId_smsLastFour ON sms_account_overrides (templateId, smsLastFour)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sms_account_overrides_instrument_smsLastFour ON sms_account_overrides (instrument, smsLastFour)",
                )
            }
        }

    val MIGRATION_28_29 =
        object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE accounts ADD COLUMN smsAutoCreated INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE accounts ADD COLUMN smsAutoCreatedUserModified INTEGER NOT NULL DEFAULT 0",
                )
            }
        }
}
